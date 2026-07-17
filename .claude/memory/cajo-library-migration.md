# Cajo replacement — implementation plan (Issue #307, Part 2)

Goal: **remove `gnu.cajo:cajo:1.117` entirely** and replace the distributed
per-test coverage transport with a **minimal, dependency-free TCP protocol** that
transmits only a fixed, whitelisted set of simple-typed messages. No new third-party
library. Must support **many clients** (dozens) connected to one server.

---

## 1. What is actually sent remotely (verified in clover-runtime)

The entire remote surface is **two events**, defined today in
`org.openclover.runtime.remote.RpcMessage.METHODS`:

| Opcode | Method (`org_openclover_runtime.Clover`) | Args |
|--------|------------------------------------------|------|
| START (1) | `allRecordersSliceStart` | `String type`, `int slice`, `long startTime` |
| END (2)   | `allRecordersSliceEnd`   | `String type`, `String method`, `String runtimeTestName` (nullable), `int slice`, `int p`, `ErrorInfo ei` (nullable) |

`org.openclover.runtime.ErrorInfo` = `{ String message; String stackTrace; }`
(both nullable). **Every field is a primitive or String** — trivially encodable.

Direction / flow (verified):
- The **server** (JVM with `-Dclover.server=true`, the test runner) produces these
  events: `Clover$InitialisedRuntime.allRecordersSliceStart/End` →
  `DistributedClover.remoteFlush(RpcMessage)` → `RecorderService.sendMessage` →
  broadcast to all connected clients.
- Each **client** (an app-under-test JVM) receives them and applies locally via
  `Clover.allRecordersSliceStart/End`.
- Client → server carries **no coverage data** — only a registration handshake.
- No coverage bitmaps ever cross the wire; each JVM keeps its own recorder.

## 2. The whitelist: a hardcoded opcode switch

The remote set is **closed and tiny (2 methods)** and already effectively hardcoded
in `RpcMessage.METHODS`. Dispatch is a `switch (opcode)` that reads exactly the
declared fields and calls the concrete method — no reflection, no name→method
lookup. That switch **is** the whitelist: an unknown opcode is rejected, and there
is no path from wire bytes to an arbitrary method or class. This is the core
security property, so it must be explicit and auditable, i.e. hardcoded.

Drift guard (cheap): a unit test that reflectively asserts
`Clover.allRecordersSliceStart(String,int,long)` and
`allRecordersSliceEnd(String,String,String,int,int,ErrorInfo)` still exist, so a
signature change forces a codec update (replacing today's "remember to update
RpcMessage#METHODS" comments). Reflection stays in *tests only*, never on the wire.

## 3. Classes

All in `clover-runtime`, package `org.openclover.runtime.remote`.

### New
- **`TcpRecorderService implements RecorderService`** (server) — replaces
  `CajoTcpRecorderService`.
  Fields: `DistributedConfig config`, `ServerSocket serverSocket`,
  `CopyOnWriteArrayList<ClientConnection> clients`, `Thread acceptThread`,
  `volatile boolean running`, a barrier monitor.
  Methods: `init(Config)`, `start()` (bind + accept loop + wait-for-N barrier),
  `stop()` (stop accepting, close all clients), `sendMessage(RpcMessage)`
  (encode once → **broadcast to every client in parallel and block until all ACK**,
  timeout → drop the laggard; see §7). Uses a small `ExecutorService` for the fan-out.
- **`TcpRecorderListener implements RecorderListener`** (client) — replaces
  `CajoTcpRecorderListener`.
  Fields: `DistributedConfig config`, `volatile Socket socket`, reconnect `Timer`,
  reader `Thread`, `AtomicBoolean connected`.
  Methods: `init`, `connect()` (schedule reconnect), `disconnect()`,
  `connectOnce()` (open socket, send REGISTER, spawn reader), reader loop →
  `MessageCodec.decodeAndDispatch`. `handleMessage` retained as a no-op for the
  interface (dead once cajo is gone; can be removed with the interface later).
- **`ClientConnection`** (server-side, one per client): `Socket`,
  `DataOutputStream out`, `DataInputStream in`. `sendAndAwaitAck(byte[], timeout)`
  writes the frame, flushes, and blocks for the client's `ACK` byte (this is the
  per-client half of the barrier). Because slice boundaries are serialized there is
  at most one in-flight event per client, so no outbound queue is needed. Disconnect
  is detected when the write or the ACK read fails (EOF/IOException/timeout).
  `closeQuietly()`.
- **`MessageCodec`** (the protocol): constants `MAGIC`, `VERSION`, `OP_SLICE_START`,
  `OP_SLICE_END`; a small `handshake` read/write pair; `byte[] encode(RpcMessage)`;
  `void decodeAndDispatch(DataInputStream)`
  (reads opcode, reads exactly the declared fields, calls the whitelisted method);
  helpers `writeNullableUTF/readNullableUTF` (a present-flag byte, so `null` stays
  distinct from `""`) and `writeErrorInfo/readErrorInfo` (an `errPresent` byte plus
  two nullable UTFs).

### Changed
- **`RemoteFactory`** — swap the two hardcoded `Class.forName` names to
  `TcpRecorderService` / `TcpRecorderListener`. (Optionally read an override system
  property to allow rollback; not required.)
- **`RpcMessage`** — keep as an in-process value holder (opcode + args in declared
  order) but **drop `implements Serializable`**; it is never serialized as an
  object again — `MessageCodec` encodes it field-by-field.
- **`ErrorInfo`** — unchanged class; simply no longer serialized (encoded as two
  nullable UTFs). Can keep `Serializable` (used elsewhere) but it is irrelevant to
  the wire now.

### Deleted
- `CajoTcpRecorderService`, `CajoTcpRecorderListener`.

### Unchanged
- `RecorderService`, `RecorderListener`, `Config`, `RemoteServiceProvider`,
  `DistributedClover`, `DistributedConfig`, `InitStringData`.

## 4. Wire format (no ObjectInputStream) — kept deliberately minimal

`java.io.DataInputStream`/`DataOutputStream` over the raw socket. `writeUTF`
length-prefixes strings; ints/longs are fixed-width. Frames are self-delimiting by
reading exactly the fields each opcode declares. There are only **three** things on
the wire: one registration line and two event types.

```
Handshake (once, on connect):
    client→server: int MAGIC, int VERSION, UTF name
    server→client: int MAGIC, int VERSION            // reply; proves a real Clover server answered

Slice events (server→client):
    byte OP=1 (START): UTF type, int slice, long startTime
    byte OP=2 (END):   UTF type, nUTF method, nUTF runtimeTestName,
                       int slice, int p, byte errPresent [, nUTF message, nUTF stackTrace]

Acknowledgement (client→server, after the client has APPLIED each slice event):
    byte ACK

Disconnect: just close the socket (peer sees EOF).
```

**The ACK is what keeps clients in lockstep** (see §7). After a client applies a
START/END locally it writes back one `ACK` byte; the server's `sendMessage` does not
return until every client has ACKed (or is dropped on timeout). Only one event is in
flight at a time (slice boundaries are serialized), so a bare ACK needs no slice id
to correlate.

`MAGIC` is a fixed constant (e.g. `0x434C5652`, "CLVR"). Both sides read it first
and **disconnect immediately on mismatch** — this guards against an unrelated
process that accidentally connects to the port (server side) or a non-Clover
service answering at the configured `host:port` (client side). It is exchanged
**only in the handshake**, not per slice event: repeating it on every frequent
event would be needless overhead, and a mid-stream framing error already forces a
disconnect. `VERSION` follows the magic so incompatible peers also fail fast.

`nUTF` = **nullable string**: `writeBoolean(present)`, then `writeUTF(s)` only if
present (`writeUTF` cannot encode `null`). Used for the genuinely optional fields —
`method`, `runtimeTestName`, and the `ErrorInfo` strings — so `null` round-trips as
`null` and stays distinct from `""`. `type` is always present (plain `UTF`).
`ErrorInfo` itself is optional: a single `errPresent` byte gates whether its two
`nUTF` fields follow (absent ⇒ `ei == null`).

Deliberately **omitted** to keep it simple (and why we can):
- **No PING** — TCP plus write-failure detection already surfaces dead peers, and
  slice events flow frequently during a run.
- **No BYE** — closing the socket (EOF) is the disconnect signal for both sides.

Bad `MAGIC`/`VERSION`, or an unknown opcode → log and close the connection; never
dispatch.

## 5. Execution flow

**Server** (`clover.server=true`):
1. First instrumented method → `Clover.getRecorder` → `new DistributedClover(server)`
   → `createServer` → `service.start()`.
2. `start()` binds `ServerSocket` to `config.getHost():getPort()` (backlog sized for
   client count); starts the accept thread; if `numClients > 0`, blocks until that
   many clients have completed the handshake (barrier), logging progress; else proceeds.
3. Accept thread: per accepted socket → read handshake (validate `MAGIC` + `VERSION`,
   read `name`; on mismatch close and skip), reply `MAGIC` + `VERSION` → create
   `ClientConnection` (start writer thread) → add to `clients` → signal barrier.
4. Test run: each slice boundary → `remoteFlush` → `sendMessage` → `encode` once →
   submit `sendAndAwaitAck` for every `ClientConnection` to the fan-out pool → block
   until all ACK (or a client times out and is dropped). Only then does the flush
   return, so the test proceeds knowing every client has applied the event.
5. `stop()`: `running=false`; close `ServerSocket`; close each client socket
   (EOF signals the clients); interrupt/join writer threads.

**Client** (no `clover.server`):
1. First instrumented method → `new DistributedClover(client)` → `createClient` →
   `listener.connect()`.
2. `connect()` schedules a reconnect task every `retryPeriod` (reuse existing
   pattern). Task: open `Socket(host,port)`, send handshake (`MAGIC` + `VERSION` +
   `name`), read the server's `MAGIC` + `VERSION` reply (disconnect on mismatch),
   start reader thread; on success cancel the timer.
3. Reader loop: `MessageCodec.decodeAndDispatch(in)` → `Clover.allRecordersSlice*`
   locally → **write one `ACK` byte back**. On `EOF`/`IOException` → close and
   reschedule reconnect (server may have restarted).
4. `disconnect()`: cancel timer, close socket, stop reader.

## 6. Connect / disconnect / error handling

| Event | Handling |
|-------|----------|
| Client can't reach server yet | reconnect timer retries every `retryPeriod` (unchanged behaviour) |
| Bad `MAGIC`/`VERSION` at handshake (either side) | disconnect immediately; guards against a stray process on the port or a non-Clover server answering; the app is not affected (coverage just disabled for that peer) |
| Unknown/corrupt opcode | receiver logs and closes that connection; never dispatches |
| Server write fails, or a client doesn't ACK within `timeout` | drop that `ClientConnection` (close, remove); the barrier completes for the rest (mirrors today's "remove failing proxy") |
| Client read EOF (server gone / shut down) | close, resume reconnect loop |
| Shutdown | just close sockets; EOF is the disconnect signal both directions |
| Barrier never reached | block with progress logging (current behaviour); optionally honour `timeout` as a max wait then proceed/fail (decide in review) |

All network exceptions are contained in the remote layer — **instrumentation/tests
must never fail because coverage transport failed** (log and continue), preserving
current fail-soft behaviour.

## 7. Performance & cross-client consistency (dozens of clients)

**Correctness requirement first:** every slice event must be **applied by all
clients before the test proceeds**. The test runner primes `START(testN)`, then
exercises each client webapp over a *separate* channel (e.g. HTTP) with no ordering
relative to the coverage channel — so a client that has only *received* (not yet
*applied*) `START` would misattribute coverage, and clients could split-brain (some
on `test1`, some already on `test2`). Therefore `sendMessage` **must be a barrier**:
it blocks until all clients confirm they applied the event. Merely writing bytes to
a socket buffer is not sufficient; an application-level **ACK** is required. This is
exactly what cajo guarantees (its synchronous request/response `invoke`).

How cajo does it (the baseline): `invokeAllClients` calls each client
**serially and synchronously**, waiting for each to execute and return, with a
per-client timeout, dropping failures — all while `remoteFlush` holds
`synchronized (RECORDERS)`. Correct, but latency is the **sum** of per-client
round-trips under the global lock, which is the bottleneck for dozens of clients.

New design — keep the barrier, make it parallel:
- `sendMessage` **encodes once** into a small immutable `byte[]`, then fans the
  `sendAndAwaitAck` out across all clients **concurrently** (an `ExecutorService`),
  and waits for all ACKs on a `CountDownLatch` with an overall `timeout`.
- Latency becomes the **slowest client (max)**, not the sum — strictly better than
  cajo while preserving the identical lockstep guarantee.
- A client that doesn't ACK within `timeout` is **dropped** (closed + removed), same
  fail-soft policy as cajo; the run continues for the rest.
- Only one event is in flight at a time (slice boundaries are serialized), so there
  is no per-client queue and no reordering to manage — TCP ordering per connection
  plus the barrier give global order for free.
- **Lock-hold reduction (optional):** the parallel broadcast+await can run *outside*
  `synchronized (RECORDERS)` (guarded by its own broadcast lock to keep global order),
  so local recorders on the server aren't blocked during the network barrier. Decide
  in review; the current cajo code holds the lock throughout.
- **Threads:** a bounded pool (or worker-per-connection) is fine for "dozens". For
  hundreds+, switch the fan-out to NIO — the protocol is unchanged (future option).
- **ACK-per-END:** `START` strictly needs the barrier; barriering `END` too is
  simplest and matches cajo. It can be made async as an optimization (the next
  `START` barrier plus TCP ordering subsumes it), but default to barriering both.

## 8. Build / packaging removal of cajo

- `pom.xml` (root) dependencyManagement: remove `gnu.cajo:cajo`.
- `clover-runtime/pom.xml`: remove the compile dependency **and** the shade
  relocation `gnu.cajo.** -> clover.gnu.cajo.**`.
- `clover-core-libs/cajo/*`: delete the vendored jar + `install-file` module;
  remove `cajo.ver` from `clover-core-libs/versions.xml`.
- Eclipse plugin `Export-Package`: drop `clover.gnu.cajo*`
  (`clover-eclipse/org.openclover.eclipse.core/pom.xml`).
- Any `clover-all` / `clover-groovy` cajo excludes: remove.

## 9. Tests & verification

- **Unit** (`clover-core` remote tests, replacing `Cajo*Test`):
    - `MessageCodec` round-trip for START and END, including **null** `runtimeTestName`
      and **null** `ErrorInfo` and null ErrorInfo fields.
    - Unknown opcode / bad magic → connection refused, no dispatch.
    - Multi-client broadcast: N clients all receive ordered START/END.
    - Barrier: server waits for `numClients`.
    - Reconnect after server restart; slow-client backpressure → drop, others fine.
    - Drift-guard test asserting the two `Clover` method signatures exist.
- **Functional:** existing Ant distributed-coverage test
  (`tests-functional/.../clover-setup-profiles-correct.xml`) passes unchanged.

## 10. Compatibility note

The wire protocol changes; all participating JVMs must run the new OpenClover
version (already a requirement for distributed coverage). Bump the `VERSION`
constant so mismatched peers fail fast at REGISTER. Target fix release **5.0.1**.

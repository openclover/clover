package org.openclover.core

import com.atlassian.clover.Contract
import com.atlassian.clover.ContractFailedException
import junit.framework.TestCase

class ContractTest extends TestCase {

    ContractTest(String name) {
        super(name)
    }

    void testContracts() {
        try {
            try {
                Contract.check(true, "true is now false. great.")
                Contract.pre(true)
                Contract.post(true)
            }
            catch (ContractFailedException cfe) {
                fail("unexpected contract failure")
            }
            try {
                Contract.check(false, "")
                fail("contract didn't fail as expected")
            }
            catch (ContractFailedException cfe) {
            }
            try {
                Contract.pre(false)
                fail("contract didn't fail as expected")
            }
            catch (ContractFailedException cfe) {
            }
            try {
                Contract.post(false)
                fail("contract didn't fail as expected")
            }
            catch (ContractFailedException cfe) {
            }
        }
        catch (Exception e) {
            fail("unexpected exception")
        }
    }
}

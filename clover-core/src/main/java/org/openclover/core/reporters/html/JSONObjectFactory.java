package org.openclover.core.reporters.html;

import org.openclover.core.api.registry.ClassInfo;
import org.openclover.core.api.registry.FileInfo;
import org.openclover.core.api.registry.MethodInfo;
import org.openclover.core.registry.entities.FullClassInfo;
import org.openclover.core.registry.entities.FullFileInfo;
import org.openclover.core.registry.entities.TestCaseInfo;
import org.openclover.core.reporters.json.JSONException;
import org.openclover.core.reporters.json.JSONObject;
import org.openclover.core.spi.reporters.html.source.LineRenderInfo;
import org.openclover.runtime.Logger;

import java.util.BitSet;
import java.util.List;
import java.util.Map;

import static org.openclover.core.util.Lists.newArrayList;
import static org.openclover.core.util.Maps.newHashMap;

public class JSONObjectFactory {
    // JSON: {test_ID : {"methods": [ID1, ID2, ID3...], "name" : "testXXX() void"}, ...};
    public static JSONObject getJSONTestTargets(Map<TestCaseInfo, BitSet> targetMethods,
                                                Map<TestCaseInfo, BitSet> targetElements) throws JSONException {
        JSONObject jsonTestTargets = new JSONObject();

        for (TestCaseInfo testcase : targetMethods.keySet()) {
            BitSet methodSet = targetMethods.get(testcase);
            BitSet elementSet = targetElements.get(testcase);

            Map<String, Object> test = newHashMap();
            test.put("pass", testcase.isSuccess());
            test.put("name", testcase.getTestName());

            List<Map<String, Integer>> methods = newArrayList();
            for (int i = methodSet.nextSetBit(0); i >= 0; i = methodSet.nextSetBit(i + 1)) {
                Map<String, Integer> method = newHashMap();
                method.put("sl", i);
                methods.add(method);
            }

            test.put("methods", methods);

            List<Map<String, Integer>> statements = newArrayList();
            if (elementSet != null) { // could be null if a test method has no statements
                for (int i = elementSet.nextSetBit(0); i >= 0; i = elementSet.nextSetBit(i + 1)) {
                    Map<String, Integer> statement = newHashMap();
                    statement.put("sl", i);
                    statements.add(statement);
                }
            }
            test.put("statements", statements);

            jsonTestTargets.put("test_" + testcase.getId(), test);
        }

        return jsonTestTargets;
    }

    // JSON: {classes : [{name, id, sl, el, methods : [{sl, el}, ...]}, ...]}
    public static JSONObject getJSONPageData(final FullFileInfo fileInfo) throws JSONException {
        final JSONObject jsonPageData = new JSONObject();

        final List<Map<String, Object>> classList = newArrayList();
        for (final ClassInfo ci : fileInfo.getClasses()) {
            final FullClassInfo classInfo = (FullClassInfo)ci;

            final Map<String, Object> classMap = newHashMap();
            classMap.put("id", classInfo.getDataIndex());
            classMap.put("sl", classInfo.getStartLine());
            classMap.put("el", classInfo.getEndLine());
            classMap.put("name", classInfo.getName());

            final List<Map<String, Integer>> methods = newArrayList();
            for (final MethodInfo methodInfo : classInfo.getMethods()) {
                final Map<String, Integer> method = newHashMap();
                method.put("sl", methodInfo.getStartLine());
                method.put("el", methodInfo.getEndLine());
                method.put("sc", methodInfo.getStartColumn());
                methods.add(method);
            }
            classMap.put("methods", methods);

            classList.add(classMap);
        }
        jsonPageData.put("classes", classList);

        return jsonPageData;
    }

    // [[],[],[testid,...],...] (line number = arrayIndex)
    public static List<List<Integer>> getJSONSrcFileLines(LineRenderInfo[] renderInfos, String name) {
        List<List<Integer>> srcFileLines = newArrayList();
        List<Integer> srcFileLine = newArrayList();
        srcFileLines.add(srcFileLine);
        for (final LineRenderInfo info : renderInfos) {
            srcFileLine = newArrayList();
            if (info == null) {
                Logger.getInstance().debug("LineRenderInfo is null for file '" + name + "'.");
                continue;
            }
            final List<TestCaseInfo> testHits = info.getTestHits();
            if (testHits == null) {
                Logger.getInstance().debug("testHits is null for file '" + name + "'.");
                continue;
            }
            for (TestCaseInfo testHit : testHits) {
                srcFileLine.add(testHit.getId());
            }
            srcFileLines.add(srcFileLine);
        }

        return srcFileLines;
    }
}

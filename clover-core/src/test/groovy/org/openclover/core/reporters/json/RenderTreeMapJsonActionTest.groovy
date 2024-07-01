package org.openclover.core.reporters.json

import org.junit.Test
import org.openclover.core.api.registry.ProjectInfo
import org.openclover.core.reporters.html.HtmlRenderingSupportImpl

import static org.junit.Assert.assertEquals

class RenderTreeMapJsonActionTest extends RenderTreeMapActionTest {

    @Test
    void testGenerateJson() throws Exception {
        final ProjectInfo project = createMockProject()
        final HtmlRenderingSupportImpl renderingSupport = new HtmlRenderingSupportImpl();
        final String json = RenderTreeMapJsonAction.generateJson(project, renderingSupport, false)

        // quick check of the json object structure
        final JSONObject jsonObj = new JSONObject(json)
        // check there is one package
        final JSONArray pkgs = jsonObj.getJSONArray("children")
        assertEquals(1, pkgs.length())
        // check there are no classes in that package
        assertEquals(0, pkgs.getJSONObject(0).getJSONArray("children").length())
    }

}

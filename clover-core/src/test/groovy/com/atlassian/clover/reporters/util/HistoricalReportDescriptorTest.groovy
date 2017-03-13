package com.atlassian.clover.reporters.util

import com.atlassian.clover.api.CloverException
import com.atlassian.clover.reporters.Historical
import org.junit.Test

import static org.junit.Assert.assertEquals

class HistoricalReportDescriptorTest {

    @Test
    void testGetBottomAdded() throws IOException, CloverException {

        // jump through some hoops to create the AddedDescriptor
        final Historical histCfg = new Historical()
        HistoricalReportDescriptor desc = new HistoricalReportDescriptor(histCfg) {
            boolean gatherHistoricalModels() {
                getAddedDescriptors().add(new HistoricalReportDescriptor.AddedDescriptor(this, null))
                return true
            }
        }
        desc.gatherHistoricalModels()

        // get and test
        HistoricalReportDescriptor.AddedDescriptor added = (HistoricalReportDescriptor.AddedDescriptor) desc.getAddedDescriptors().get(0)
        List<String> list = [ "0", "1", "2", "3", "4", "5", "6", "7", "8", "9" ]
        List bottom5 = added.getBottomMovers(list, 5)
        assertEquals(5, bottom5.size())
        assertEquals("0", bottom5.get(0))
        assertEquals("4", bottom5.get(4))

        List<String> shortList = [ "0", "1" ]

        bottom5 = added.getBottomMovers(shortList, 5)
        assertEquals(2, bottom5.size())
        assertEquals("0", bottom5.get(0))
        assertEquals("1", bottom5.get(1))
    }
}

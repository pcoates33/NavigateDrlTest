package com.pcoates33;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DrlFileGotoDeclarationHandlerTest {

    @Test
    public void extractDrlFilename() {
        DrlFileGotoDeclarationHandler onTest = new DrlFileGotoDeclarationHandler();

        assertEquals("simple1.drl", onTest.extractFilename("\"simple1.drl\"", ".drl"));
        assertEquals("simple2.drl", onTest.extractFilename(" simple2.drl", ".drl"));
        assertEquals("folder/simple3.drl", onTest.extractFilename(" folder/simple3.drl", ".drl"));
        assertEquals("simple4.drl", onTest.extractFilename(" find simple4.drl in the string", ".drl"));

        assertNull(onTest.extractFilename("no drl file here", ".drl"));
    }
}
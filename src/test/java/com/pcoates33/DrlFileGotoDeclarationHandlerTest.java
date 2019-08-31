package com.pcoates33;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DrlFileGotoDeclarationHandlerTest {

    @Test
    public void extractDrlFilename() {
        DrlFileGotoDeclarationHandler onTest = new DrlFileGotoDeclarationHandler();

        assertEquals("simple1.drl", onTest.extractDrlFilename("\"simple1.drl\""));
        assertEquals("simple2.drl", onTest.extractDrlFilename(" simple2.drl"));
        assertEquals("simple3.drl", onTest.extractDrlFilename(" folder/simple3.drl"));
        assertEquals("simple4.drl", onTest.extractDrlFilename(" find simple4.drl in the string"));

        assertNull(onTest.extractDrlFilename("no drl file here"));
    }
}
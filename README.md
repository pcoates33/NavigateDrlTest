# NavigateDrlTest
Intellij plugin to:
 - navigate to expected.json files from integration tests
 - navigate between drl files and tests annotated with DrlTest.

Run gradle task buildPlugin to build a jar that you can then install from settings - plugins.

Relies on an annotation DrlTest which should have a files parameter to provide the list of drl files used by the test.

Example annotation :

    package com.pcoates33;

    import java.lang.annotation.ElementType;
    import java.lang.annotation.Retention;
    import java.lang.annotation.RetentionPolicy;
    import java.lang.annotation.Target;

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface DrlTest {
      String[] files() default "";
    }

Example usage :

    import com.pcoates33.DrlTest;
    import org.junit.Test;

    @DrlTest(files={"First.drl", "Second.drl"})
    public class HasAnnationTest {

        @Test
        public void testOne() {
            //see if we can link to this class from First.drl
        }
    }
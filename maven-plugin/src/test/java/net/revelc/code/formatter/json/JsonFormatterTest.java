/**
 * Copyright 2010-2017. All work is copyrighted to their respective
 * author(s), unless otherwise stated.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.revelc.code.formatter.json;

import net.revelc.code.formatter.AbstractFormatterTest;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.HashMap;

/**
 * @author yoshiman
 */
public class JsonFormatterTest extends AbstractFormatterTest {

    @Test
    public void testDoFormatFile() throws Exception {
        doTestFormat(new JsonFormatter(), "someFile.json",
                "ce94186dfe66fe2813fab37d2f4f9eb6e4ca21ee6351051cd971652798b6760be75de0c7ff92913f55378003ffb72fc3ee5289aa213773144a643de891e3cb3a");
    }

    @Test
    public void testIsIntialized() throws Exception {
        JsonFormatter jsonFormatter = new JsonFormatter();
        Assert.assertFalse(jsonFormatter.isInitialized());
        final File targetDir = new File("target/testoutput");
        targetDir.mkdirs();
        jsonFormatter.init(new HashMap<String, String>(), new AbstractFormatterTest.TestConfigurationSource(targetDir));
        Assert.assertTrue(jsonFormatter.isInitialized());
    }

}

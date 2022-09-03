package com.plugin.datadogsnapshotgraph;

import com.dtolabs.rundeck.core.execution.workflow.steps.StepException;
import com.dtolabs.rundeck.core.storage.ResourceMeta;
import com.dtolabs.rundeck.plugins.step.PluginStepContext;

import java.io.ByteArrayOutputStream;

public class datadogUtil {

    static String getPasswordFromKeyStorage(String path, PluginStepContext context) throws StepException {
        try{
            ResourceMeta contents = context.getExecutionContext().getStorageTree().getResource(path).getContents();
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            contents.writeContent(byteArrayOutputStream);
            String password = new String(byteArrayOutputStream.toByteArray());

            return password;
        }catch(Exception e){
            throw new StepException("can't find the password key storage path ${path}", Datadogsnapshotgraph.Reason.ExampleReason);
        }
    }

}

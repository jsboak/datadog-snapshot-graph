package com.plugin.datadogsnapshotgraph;

import com.datadog.api.client.ApiClient;
import com.datadog.api.client.ApiException;
import com.datadog.api.client.v1.api.SnapshotsApi;
import com.datadog.api.client.v1.api.SnapshotsApi.GetGraphSnapshotOptionalParameters;
import com.datadog.api.client.v1.model.GraphSnapshot;
import com.dtolabs.rundeck.core.execution.workflow.steps.FailureReason;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepException;
import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.core.plugins.configuration.Describable;
import com.dtolabs.rundeck.core.plugins.configuration.Description;
import com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription;
import com.dtolabs.rundeck.plugins.step.PluginStepContext;
import com.dtolabs.rundeck.plugins.step.StepPlugin;
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder;
import com.dtolabs.rundeck.plugins.util.PropertyBuilder;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.plugin.datadogsnapshotgraph.datadogUtil.getPasswordFromKeyStorage;

@Plugin(service = ServiceNameConstants.WorkflowStep, name="datadog-snapshot-graph")
@PluginDescription(title= "Datadog / Snapshot Graph", description = "Creates Snapshot of Graph from Datadog")
public class Datadogsnapshotgraph implements StepPlugin, Describable {

    @Override
    public Description getDescription() {
        return DescriptionBuilder.builder()
                .name("datadog-snapshot-graph")
                .title("Datadog / Snapshot Graph")
                .description("Future description")
                .property(PropertyBuilder.builder()
                        .string("metricQuery")
                        .renderingOption("displayType","CODE")
                        .renderingOption(StringRenderingConstants.GROUP_NAME,"Snapshot Configuration")
                        .title("Metric Query")
                        .build()
                )
                .property(PropertyBuilder.builder()
                                .string("graphTitle")
                                .title("Graph Title")
                                .renderingOption(StringRenderingConstants.GROUP_NAME,"Snapshot Configuration")
                        .description("Title for this snapshot graph.")
                        .build()
                )
                .property(PropertyBuilder.builder()
                        .string("apiKey")
                        .renderingOption(StringRenderingConstants.SELECTION_ACCESSOR_KEY, "STORAGE_PATH")
                        .renderingOption(StringRenderingConstants.STORAGE_PATH_ROOT_KEY, "keys")
                        .renderingOption(StringRenderingConstants.STORAGE_FILE_META_FILTER_KEY, "Rundeck-data-type=password")
                        .renderingOption(StringRenderingConstants.GROUP_NAME,"Authentication")
                        .title("Datadog API Key")
                        .build()
                )
                .property(PropertyBuilder.builder()
                        .string("appKey")
                        .renderingOption(StringRenderingConstants.SELECTION_ACCESSOR_KEY, "STORAGE_PATH")
                        .renderingOption(StringRenderingConstants.STORAGE_PATH_ROOT_KEY, "keys")
                        .renderingOption(StringRenderingConstants.STORAGE_FILE_META_FILTER_KEY, "Rundeck-data-type=password")
                        .renderingOption(StringRenderingConstants.GROUP_NAME,"Authentication")
                        .title("Datadog App Key")
                        .build()
                )
                .build();
    }

    static enum Reason implements FailureReason{
        ExampleReason
    }

    @Override
    public void executeStep(PluginStepContext context, Map<String, Object> configuration) throws StepException {

        ApiClient defaultClient = ApiClient.getDefaultApiClient();
        HashMap<String, String> secrets = new HashMap<>();
        secrets.put("appKeyAuth", getPasswordFromKeyStorage(configuration.get("appKey").toString(), context));
        secrets.put("apiKeyAuth", getPasswordFromKeyStorage(configuration.get("apiKey").toString(), context));
        defaultClient.configureApiKeys(secrets);

        SnapshotsApi apiInstance = new SnapshotsApi(defaultClient);

        Map<String, String> meta = new HashMap<>();
        meta.put("content-data-type", "text/html");

        try {
            GraphSnapshot result =
                    apiInstance.getGraphSnapshot(
                            java.time.OffsetDateTime.now().plusHours(-1).toInstant().getEpochSecond(),
                            java.time.OffsetDateTime.now().toInstant().getEpochSecond(),
                            new GetGraphSnapshotOptionalParameters()
                                    .metricQuery(configuration.get("metricQuery").toString())
                                    .title(configuration.get("graphTitle").toString())
                                    .height(400L)
                                    .width(600L));

            TimeUnit.SECONDS.sleep(3);
            Connection.Response html = Jsoup.connect(result.getSnapshotUrl()).ignoreContentType(true).execute();

            while (!Objects.equals(html.contentType(), "image/png")) {

//                System.out.println(html.contentType());
                System.out.println("Snapshot still building");
                html = Jsoup.connect(result.getSnapshotUrl()).ignoreContentType(true).execute();
                TimeUnit.SECONDS.sleep(5);
            }

            String html_out = "<body style=\"margin: 0px; background: #0e0e0e; height: 100%\"><img style=\"display: block;-webkit-user-select: none;margin: auto;cursor: zoom-in;background-color: hsl(0, 0%, 90%);transition: background-color 300ms;\" src=\"" + result.getSnapshotUrl() + "\" width=\"518\" height=\"345\"></body>";

            context.getLogger().log(2, html_out, meta);

        } catch (ApiException e) {
            System.err.println("Exception when calling SnapshotsApi#getGraphSnapshot");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        } catch (IOException | InterruptedException io) {
            io.printStackTrace();
        }
    }

}

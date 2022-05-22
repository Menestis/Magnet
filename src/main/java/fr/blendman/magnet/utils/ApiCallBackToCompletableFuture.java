package fr.blendman.magnet.utils;

import fr.blendman.skynet.client.ApiCallback;
import fr.blendman.skynet.client.ApiException;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @author Blendman974
 */
public class ApiCallBackToCompletableFuture<U> implements ApiCallback<U> {
    private final CompletableFuture<U> completableFuture;

    public ApiCallBackToCompletableFuture(CompletableFuture<U> completableFuture) {
        this.completableFuture = completableFuture;
    }

    @Override
    public void onFailure(ApiException e, int statusCode, Map<String, List<String>> responseHeaders) {
        completableFuture.completeExceptionally(e);
        System.out.println(e.getResponseBody());
    }

    @Override
    public void onSuccess(U result, int statusCode, Map<String, List<String>> responseHeaders) {
        completableFuture.complete(result);
    }

    @Override
    public void onUploadProgress(long bytesWritten, long contentLength, boolean done) {

    }

    @Override
    public void onDownloadProgress(long bytesRead, long contentLength, boolean done) {

    }
}

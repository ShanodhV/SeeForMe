package com.shanodh.seeforme.network;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

/**
 * Retrofit interface for API calls to the Orange Pi 5 wearable device
 */
public interface ApiService {
    /**
     * Add a note to the wearable device for OCR matching
     * @param note Note object containing the text to match
     * @return API response
     */
    @POST("/add-note")
    Call<ApiResponse> addNote(@Body Map<String, String> note);

    /**
     * Add a familiar face to the wearable device
     * @param image Image file part
     * @param name Name of the person
     * @return API response
     */
    @Multipart
    @POST("/add-face")
    Call<ApiResponse> addFace(
            @Part MultipartBody.Part image,
            @Part("name") String name,
            @Part("relationship") String relationship
    );

    /**
     * Get the status of the wearable device
     * @return Status response
     */
    @GET("/status")
    Call<ApiResponse> getStatus();

    /**
     * Get notifications from the wearable device
     * @return List of notifications
     */
    @GET("/notifications")
    Call<List<NotificationResponse>> getNotifications();
} 
package com.laba.sergo;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Query;

public interface MeteostatApi {

    @Headers({
        "X-RapidAPI-Key: f588b4e3bfmsh6739b8f50b38334p12c1d9jsna343a676d8b1",
        "X-RapidAPI-Host: meteostat.p.rapidapi.com"
    })
    @GET("point/daily")
    Call<WeatherResponse> getDailyForecast(
            @Query("lat") double latitude,
            @Query("lon") double longitude,
            @Query("start") String startDate,
            @Query("end") String endDate
    );
}

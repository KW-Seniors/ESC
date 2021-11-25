package kr.co.kw_seniors.endsemesterclock

import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import com.google.gson.annotations.SerializedName
import kr.co.kw_seniors.endsemesterclock.databinding.ActivityWeatherBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.io.IOException
import java.util.*
import java.util.jar.Manifest
import kotlin.collections.ArrayList

class WeatherActivity : AppCompatActivity() {

    companion object {
        var BaseUrl = "http://api.openweathermap.org/"
        var AppId = "745cc7df93b0641772ea3cd85925e2cb"//https://home.openweathermap.org 에서의 키값
        var lat = "37.445293" //위도 좌표
        var lon = "126.785823" //경도 좌표
        /*
        var locationNManager: LocationManager? = null
        val REQUIRED_PERMISSIONS =
            arrayOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            )
        val PERMISSIONS_REQUEST_CODE = 100
         */
    }

    val binding by lazy { ActivityWeatherBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        //Create Retrofit Builder
        val retrofit = Retrofit.Builder()
            .baseUrl(BaseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(WeatherService::class.java)
        val call = service.getCurrentWeatherData(lat, lon, AppId)
        call.enqueue(object : Callback<WeatherResponse> {
            override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                Log.d("MainActivity", "result :" + t.message)
            }

            override fun onResponse(
                call: Call<WeatherResponse>,
                response: Response<WeatherResponse>
            ) {
                if (response.code() == 200) {
                    val weatherResponse = response.body()
                    Log.d("MainActivity", "result: " + weatherResponse.toString())
                    var cTemp = weatherResponse!!.main!!.temp - 273.15  //켈빈을 섭씨로 변환
                    var minTemp = weatherResponse!!.main!!.temp_min - 273.15
                    var maxTemp = weatherResponse!!.main!!.temp_max - 273.15
                    val stringBuilder =
                        "지역: " + weatherResponse!!.sys!!.country + "\n" +
                                "현재기온: " + cTemp + "\n" +
                                "최저기온: " + minTemp + "\n" +
                                "최고기온: " + maxTemp + "\n" +
                                "풍속: " + weatherResponse!!.wind!!.speed + "\n" +
                                "일출시간: " + weatherResponse!!.sys!!.sunrise + "\n" +
                                "일몰시간: " + weatherResponse!!.sys!!.sunset + "\n" +
                                "아이콘: " + weatherResponse!!.weather!!.get(0).icon + "\n"

                    Log.d("WeatherApi", stringBuilder)
                }
            }

        })

        /*
        //현재 좌표를 얻기 위한 함수
        fun getLatLng(): Location {
            var currentLatLng: Location? = null
            var hasFineLocationPermission = ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            )
            var hasCoarseLocationPermission = ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            )

            if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED &&
                hasCoarseLocationPermission == PackageManager.PERMISSION_GRANTED
            ) {
                val locatioNProvider = LocationManager.GPS_PROVIDER
                currentLatLng = locatioNManager?.getLastKnownLocation(locatioNProvider)
            } else {
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        REQUIRED_PERMISSIONS[0]
                    )
                ) {
                    Toast.makeText(this, "앱을 실행하려면 위치 접근 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
                    ActivityCompat.requestPermissions(
                        this,
                        REQUIRED_PERMISSIONS,
                        PERMISSIONS_REQUEST_CODE
                    )
                } else {
                    ActivityCompat.requestPermissions(
                        this,
                        REQUIRED_PERMISSIONS,
                        PERMISSIONS_REQUEST_CODE
                    )
                }
                currentLatLng = getLatLng()
            }
            return currentLatLng!!
        }

        //좌표를 출력하는 함수
        fun getLocation() {
            locatioNManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager?
            var userLocation: Location = getLatLng()
            if (userLocation != null) {
                lat = userLocation.latitude.toString()
                lon = userLocation.longitude.toString()
                Log.d("CheckCurrentLocation", "현재 내 위치 값: ${lat}, ${lon}")
            }
            else{
                Log.d("CheckCurrentLocation", "실패!")
            }
        }
        */
    }
}

interface WeatherService{

    @GET("data/2.5/weather")
    fun getCurrentWeatherData(
        @Query("lat") lat: String,
        @Query("lon") lon: String,
        @Query("appid") appid: String) :
            Call<WeatherResponse>
}

class WeatherResponse(){
    @SerializedName("weather") var weather = ArrayList<Weather>()
    @SerializedName("main") var main: Main? = null
    @SerializedName("wind") var wind : Wind? = null
    @SerializedName("sys") var sys: Sys? = null
}

class Weather {
    @SerializedName("id") var id: Int = 0
    @SerializedName("main") var main : String? = null
    @SerializedName("description") var description: String? = null
    @SerializedName("icon") var icon : String? = null
}

class Main {
    @SerializedName("temp")
    var temp: Float = 0.toFloat()
    @SerializedName("humidity")
    var humidity: Float = 0.toFloat()
    @SerializedName("pressure")
    var pressure: Float = 0.toFloat()
    @SerializedName("temp_min")
    var temp_min: Float = 0.toFloat()
    @SerializedName("temp_max")
    var temp_max: Float = 0.toFloat()

}

class Wind {
    @SerializedName("speed")
    var speed: Float = 0.toFloat()
    @SerializedName("deg")
    var deg: Float = 0.toFloat()
}

class Sys {
    @SerializedName("country")
    var country: String? = null
    @SerializedName("sunrise")
    var sunrise: Long = 0
    @SerializedName("sunset")
    var sunset: Long = 0
}

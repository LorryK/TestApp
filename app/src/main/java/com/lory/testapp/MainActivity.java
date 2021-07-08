package com.lory.testapp;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.here.android.mpa.common.GeoBoundingBox;
import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.guidance.NavigationManager;
import com.here.android.mpa.mapping.AndroidXMapFragment;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapMarker;
import com.here.android.mpa.mapping.MapObject;
import com.here.android.mpa.mapping.MapRoute;
import com.here.android.mpa.prefetcher.MapDataPrefetcher;
import com.here.android.mpa.routing.CoreRouter;
import com.here.android.mpa.routing.Route;
import com.here.android.mpa.routing.RouteOptions;
import com.here.android.mpa.routing.RoutePlan;
import com.here.android.mpa.routing.RouteResult;
import com.here.android.mpa.routing.RouteWaypoint;
import com.here.android.mpa.routing.RoutingError;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class MainActivity extends AppCompatActivity {
    private AndroidXMapFragment mMapFragment;
    private Map mMap;
    private ArrayList<GeoCoordinate> data = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mMapFragment = (AndroidXMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapfragment);
        initMapFragment();
    }

    private void initMapFragment() {
        mMapFragment.init(new OnEngineInitListener() {

            @Override
            public void onEngineInitializationCompleted(Error error) {
                if (error == Error.NONE) {
                    mMap = mMapFragment.getMap();
                    getCoordinates();
                }
            }
        });
    }

    private void getCoordinates() {
        RequestController.getInstance()
                .getGeoRequest()
                .getCoordinates()
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        ResponseBody textResponse = response.body();
                        String[] result = null;
                        try {
                            result = textResponse.string().split("\\R");
                        } catch (IOException e) {
//                            Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
                        }

                        data = parseCoordinates(result);

                        if (data.size() != 0) {
                            addMarkers();

                            GeoBoundingBox geoBoundingBox = GeoBoundingBox.getBoundingBoxContainingGeoCoordinates(data);
                            mMap.zoomTo(geoBoundingBox, Map.Animation.NONE, 7);

                            MapDataPrefetcher prefetcher = MapDataPrefetcher.getInstance();
                            MapDataPrefetcher.Request request = prefetcher.fetchMapData(geoBoundingBox);

                            Toast.makeText(MainActivity.this, "Start loading map", Toast.LENGTH_SHORT).show();

                            prefetcher.addListener(new MapDataPrefetcher.Adapter() {
                                @Override
                                public void onProgress(int i, float v) {
                                    Toast.makeText(MainActivity.this, String.format(Locale.getDefault(), "Map loaded: %.2f%%", v), Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onStatus(int i, PrefetchStatus prefetchStatus) {
                                    if (request.requestId == i && prefetchStatus == PrefetchStatus.PREFETCH_SUCCESS) {
                                        calculateCustomRoute();
                                    }
                                }
                            });
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        Toast.makeText(MainActivity.this, "Error occured: " + t.toString(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private ArrayList<GeoCoordinate> parseCoordinates(String[] coordinates) {
        ArrayList<GeoCoordinate> coord = new ArrayList<>();
        String latitude, longitude;
        for (int i = 0; i < coordinates.length; i++) {
            latitude = coordinates[i].substring(0, coordinates[i].indexOf(","));
            longitude = coordinates[i].substring(coordinates[i].indexOf(",") + 1);
            coord.add(new GeoCoordinate(Double.parseDouble(latitude), Double.parseDouble(longitude)));
        }
        return coord;
    }

    private void addMarkers() {
        ArrayList<MapObject> mapMarkers = new ArrayList<>();
        for (int i = 0; i < data.size(); i++) {
            mapMarkers.add(new MapMarker(data.get(i)));
        }

        mMap.addMapObjects(mapMarkers);
    }

    private void calculateCustomRoute() {

        RoutePlan routePlan = new RoutePlan();
        routePlan.addWaypoint(new RouteWaypoint(data.get(0)));
        routePlan.addWaypoint(new RouteWaypoint(data.get(1)));
        routePlan.addWaypoint(new RouteWaypoint(data.get(2)));

        RouteOptions routeOptions = new RouteOptions()
                .setTransportMode(RouteOptions.TransportMode.CAR)
                .setRouteType(RouteOptions.Type.FASTEST);

        routePlan.setRouteOptions(routeOptions);

        CoreRouter router = new CoreRouter();

        router.calculateRoute(routePlan, new CoreRouter.Listener() {
            @Override
            public void onCalculateRouteFinished(List<RouteResult> routeResults, RoutingError routingError) {
                if (routingError == RoutingError.NONE) {
                    Route route = routeResults.get(0).getRoute();
                    mMap.addMapObject(new MapRoute(route));
                    startNavigation(route);
                } else {
                    Toast.makeText(MainActivity.this, "Error occured: " + routingError.toString(),
                            Toast.LENGTH_SHORT).show();
                    Log.e("Error ", routingError.toString());
                }
            }

            @Override
            public void onProgress(int percentage) {
                Toast.makeText(MainActivity.this, "Route calculation in progress " + percentage + "%",
                    Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startNavigation(Route route) {
        NavigationManager navigationManager = NavigationManager.getInstance();
        navigationManager.simulate(route, 16);
    }
}
package com.backdoor.moove.modern_ui.map

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.location.Address
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RawRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import com.backdoor.moove.R
import com.backdoor.moove.data.Place
import com.backdoor.moove.databinding.MapFragmentBinding
import com.backdoor.moove.modern_ui.map.list.RecentPlacesAdapter
import com.backdoor.moove.utils.*
import com.backdoor.moove.views.AddressAutoCompleteView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import org.koin.android.ext.android.inject
import timber.log.Timber

class MapFragment : Fragment() {

    private val prefs: Prefs by inject()
    private val coloring: Coloring by inject()
    private val dialogues: Dialogues by inject()

    private var mMap: GoogleMap? = null
    private var placeRecyclerAdapter = RecentPlacesAdapter()

    private lateinit var binding: MapFragmentBinding
    private lateinit var viewModel: MapViewModel

    private var mMapType = GoogleMap.MAP_TYPE_NORMAL
    private var isTouch = true
    private var isBack = true
    private var isStyles = true
    private var isPlaces = true
    private var isSearch = true
    private var isRadius = true

    private var markerTitle: String = ""
    var markerRadius = -1
    var markerStyle = -1
        private set
    private var mMarkerStyle: Drawable? = null
    private var lastPos: LatLng? = null
    private val strokeWidth = 3f

    private var mListener: MapListener? = null
    private var mCallback: MapCallback? = null

    private var onMapClickListener: GoogleMap.OnMapClickListener? = null
    private var onMarkerClickListener: GoogleMap.OnMarkerClickListener? = null

    private val mMapCallback = OnMapReadyCallback { googleMap ->
        mMap = googleMap
        googleMap.uiSettings.isMyLocationButtonEnabled = false
        googleMap.uiSettings.isCompassEnabled = true
        setStyle(googleMap, prefs.mapType)
        setMyLocation()
        googleMap.setOnMapClickListener {
            hideLayers()
            hideStyles()
            onMapClickListener?.onMapClick(it)
        }
        setOnMarkerClick(onMarkerClickListener)
        if (lastPos != null) {
            addMarker(lastPos, lastPos.toString(), true, false, markerRadius)
        }
        mCallback?.onMapReady()
    }

    private val isLayersVisible: Boolean
        get() = binding.layersContainer.visibility == View.VISIBLE

    private val isStylesVisible: Boolean
        get() = binding.mapStyleContainer.visibility == View.VISIBLE

    fun setSearchEnabled(enabled: Boolean) {
        if (enabled) {
            binding.searchCard.visibility = View.VISIBLE
        } else {
            binding.searchCard.visibility = View.GONE
        }
    }

    fun setListener(listener: MapListener) {
        this.mListener = listener
    }

    fun setCallback(callback: MapCallback) {
        this.mCallback = callback
    }

    fun addMarker(pos: LatLng?, title: String?, clear: Boolean, animate: Boolean, radius: Int = markerRadius) {
        var t = title
        if (mMap != null && pos != null) {
            markerRadius = radius
            if (markerRadius == -1)
                markerRadius = prefs.radius
            if (clear) mMap?.clear()
            if (t == null || t == "") t = pos.toString()
            lastPos = pos
            mListener?.placeChanged(pos, t)
            mMap?.addMarker(MarkerOptions()
                    .position(pos)
                    .title(t)
                    .icon(BitmapUtils.getDescriptor(mMarkerStyle!!))
                    .draggable(clear))
            val marker = coloring.getMarkerRadiusStyle(markerStyle)
            mMap?.addCircle(CircleOptions()
                    .center(pos)
                    .radius(markerRadius.toDouble())
                    .strokeWidth(strokeWidth)
                    .fillColor(coloring.getColor(marker.fillColor))
                    .strokeColor(coloring.getColor(marker.strokeColor)))
            if (animate) animate(pos)
        }
    }

    fun addMarker(pos: LatLng, title: String?, clear: Boolean, markerStyle: Int, animate: Boolean, radius: Int): Boolean {
        var t = title
        if (mMap != null) {
            markerRadius = radius
            if (markerRadius == -1) {
                markerRadius = prefs.radius
            }
            this.markerStyle = markerStyle
            createStyleDrawable()
            if (clear) mMap?.clear()
            if (t == null || t.matches("".toRegex())) t = pos.toString()
            lastPos = pos
            mListener?.placeChanged(pos, t)
            mMap?.addMarker(MarkerOptions()
                    .position(pos)
                    .title(t)
                    .icon(BitmapUtils.getDescriptor(mMarkerStyle!!))
                    .draggable(clear))
            val marker = coloring.getMarkerRadiusStyle(this.markerStyle)
            mMap?.addCircle(CircleOptions()
                    .center(pos)
                    .radius(markerRadius.toDouble())
                    .strokeWidth(strokeWidth)
                    .fillColor(coloring.getColor(marker.fillColor))
                    .strokeColor(coloring.getColor(marker.strokeColor)))
            if (animate) animate(pos)
            return true
        } else {
            Timber.d("Map is not initialized!")
            return false
        }
    }

    private fun recreateMarker(radius: Int = markerRadius) {
        markerRadius = radius
        if (markerRadius == -1)
            markerRadius = prefs.radius
        if (mMap != null && lastPos != null) {
            mMap?.clear()
            if (markerTitle == "" || markerTitle.matches("".toRegex()))
                markerTitle = lastPos!!.toString()
            mListener?.placeChanged(lastPos!!, markerTitle)
            mMap?.addMarker(MarkerOptions()
                    .position(lastPos!!)
                    .title(markerTitle)
                    .icon(BitmapUtils.getDescriptor(mMarkerStyle!!))
                    .draggable(true))
            val marker = coloring.getMarkerRadiusStyle(markerStyle)
            mMap?.addCircle(CircleOptions()
                    .center(lastPos)
                    .radius(markerRadius.toDouble())
                    .strokeWidth(strokeWidth)
                    .fillColor(coloring.getColor(marker.fillColor))
                    .strokeColor(coloring.getColor(marker.strokeColor)))
            animate(lastPos!!)
        }
    }

    private fun recreateStyle(style: Int) {
        markerStyle = style
        createStyleDrawable()
        if (mMap != null && lastPos != null) {
            mMap?.clear()
            if (markerTitle == "" || markerTitle.matches("".toRegex()))
                markerTitle = lastPos.toString()
            mListener?.placeChanged(lastPos!!, markerTitle)
            mMap?.addMarker(MarkerOptions()
                    .position(lastPos!!)
                    .title(markerTitle)
                    .icon(BitmapUtils.getDescriptor(mMarkerStyle!!))
                    .draggable(true))
            if (markerStyle >= 0) {
                val marker = coloring.getMarkerRadiusStyle(markerStyle)
                if (markerRadius == -1) {
                    markerRadius = prefs.radius
                }
                mMap?.addCircle(CircleOptions()
                        .center(lastPos)
                        .radius(markerRadius.toDouble())
                        .strokeWidth(strokeWidth)
                        .fillColor(coloring.getColor(marker.fillColor))
                        .strokeColor(coloring.getColor(marker.strokeColor)))
            }
            animate(lastPos!!)
        }
    }

    private fun createStyleDrawable() {
        mMarkerStyle = DrawableHelper.withContext(context!!)
                .withDrawable(R.drawable.ic_twotone_place_24px)
                .withColor(coloring.accentColor(markerStyle))
                .tint()
                .get()
    }

    fun setStyle(style: Int) {
        this.markerStyle = style
    }

    fun moveCamera(pos: LatLng, i1: Int, i2: Int, i3: Int, i4: Int) {
        if (mMap != null) {
            animate(pos)
            mMap?.setPadding(i1, i2, i3, i4)
        }
    }

    private fun animate(latLng: LatLng) {
        val update = CameraUpdateFactory.newLatLngZoom(latLng, 13f)
        mMap?.animateCamera(update)
    }

    @Suppress("DEPRECATION")
    @SuppressLint("MissingPermission")
    private fun moveToMyLocation() {
        if (!Permissions.ensurePermissions(activity!!, REQ_LOC, Permissions.ACCESS_COARSE_LOCATION, Permissions.ACCESS_FINE_LOCATION)) {
            return
        }
        if (mMap != null) {
            val locationManager = context?.getSystemService(Context.LOCATION_SERVICE) as LocationManager?
            val criteria = Criteria()
            var location: Location? = null
            try {
                location = locationManager?.getLastKnownLocation(locationManager.getBestProvider(criteria, false) ?: "")
            } catch (e: IllegalArgumentException) {
                Timber.d("moveToMyLocation: ${e.message}")
            }

            if (location != null) {
                val pos = LatLng(location.latitude, location.longitude)
                animate(pos)
            } else {
                try {
                    location = mMap?.myLocation
                    if (location != null) {
                        val pos = LatLng(location.latitude, location.longitude)
                        animate(pos)
                    }
                } catch (ignored: IllegalStateException) {
                }
            }
        }
    }

    fun onBackPressed(): Boolean {
        return when {
            isLayersVisible -> {
                hideLayers()
                false
            }
            isStylesVisible -> {
                hideStyles()
                false
            }
            else -> true
        }
    }

    private fun initArgs() {
        val args = arguments
        if (args != null) {
            isTouch = args.getBoolean(ENABLE_TOUCH, true)
            isPlaces = args.getBoolean(ENABLE_PLACES, true)
            isSearch = args.getBoolean(ENABLE_SEARCH, true)
            isStyles = args.getBoolean(ENABLE_STYLES, true)
            isRadius = args.getBoolean(ENABLE_RADIUS, true)
            isBack = args.getBoolean(ENABLE_BACK, true)
            markerStyle = args.getInt(MARKER_STYLE, prefs.markerStyle)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mMapType = prefs.mapType
        initArgs()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = MapFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        markerRadius = prefs.radius
        markerStyle = prefs.markerStyle
        createStyleDrawable()
        setOnMapClickListener(GoogleMap.OnMapClickListener { latLng ->
            hideLayers()
            if (isTouch) {
                addMarker(latLng, markerTitle, true, true, markerRadius)
            }
        })

        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.getMapAsync(mMapCallback)

        initViews()

        binding.cardSearch.setOnItemClickListener { _, _, position, _ ->
            val sel = binding.cardSearch.getAddress(position) ?: return@setOnItemClickListener
            val lat = sel.latitude
            val lon = sel.longitude
            val pos = LatLng(lat, lon)
            addMarker(pos, getFormattedAddress(sel), true, true, markerRadius)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        initViewModel()
    }

    private fun initViewModel() {
        viewModel = ViewModelProviders.of(this).get(MapViewModel::class.java)
        viewModel.places.observe(this, Observer {
            if (it != null && isPlaces) {
                showPlaces(it)
            }
        })
    }

    private fun showRadiusDialog() {
        dialogues.showRadiusBottomDialog(activity!!, markerRadius) {
            recreateMarker(it)
            return@showRadiusBottomDialog getString(R.string.selected_radius_meters, it.toString())
        }
    }

    private fun showStyleDialog() {
        dialogues.showColorBottomDialog(activity!!, prefs.markerStyle, coloring.colorsForSlider()) {
            prefs.markerStyle = it
            recreateStyle(it)
        }
    }

    fun setOnMarkerClick(onMarkerClickListener: GoogleMap.OnMarkerClickListener?) {
        this.onMarkerClickListener = onMarkerClickListener
        mMap?.setOnMarkerClickListener(onMarkerClickListener)
    }

    fun setOnMapClickListener(onMapClickListener: GoogleMap.OnMapClickListener) {
        this.onMapClickListener = onMapClickListener
        mMap?.setOnMapClickListener(onMapClickListener)
    }

    private fun getFormattedAddress(address: Address): String {
        return if (address.getAddressLine(0) != null) {
            address.getAddressLine(0)
        } else {
            AddressAutoCompleteView.formName(address)
        }
    }

    private fun initViews() {
        binding.placesList.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.placesList.adapter = placeRecyclerAdapter
        LinearSnapHelper().attachToRecyclerView(binding.placesList)

        binding.placesListCard.visibility = View.GONE
        binding.layersContainer.visibility = View.GONE

        binding.layersCard.setOnClickListener { toggleLayers() }
        binding.myCard.setOnClickListener {
            hideLayers()
            hideStyles()
            moveToMyLocation()
        }
        binding.markersCard.setOnClickListener { toggleMarkers() }
        binding.radiusCard.setOnClickListener { toggleRadius() }
        binding.backCard.setOnClickListener { invokeBack() }

        binding.typeNormal.setOnClickListener { typeClick(GoogleMap.MAP_TYPE_NORMAL) }
        binding.typeSatellite.setOnClickListener { typeClick(GoogleMap.MAP_TYPE_SATELLITE) }
        binding.typeHybrid.setOnClickListener { typeClick(GoogleMap.MAP_TYPE_HYBRID) }
        binding.typeTerrain.setOnClickListener { typeClick(GoogleMap.MAP_TYPE_TERRAIN) }

        binding.styleDay.setOnClickListener { styleClick(0) }
        binding.styleRetro.setOnClickListener { styleClick(1) }
        binding.styleSilver.setOnClickListener { styleClick(2) }
        binding.styleNight.setOnClickListener { styleClick(3) }
        binding.styleDark.setOnClickListener { styleClick(4) }
        binding.styleAubergine.setOnClickListener { styleClick(5) }

        if (!isBack) {
            binding.backCard.visibility = View.GONE
        }
        if (!isSearch) {
            binding.searchCard.visibility = View.GONE
        }
        if (!isRadius) {
            binding.radiusCard.visibility = View.GONE
        }
        if (!isStyles) {
            binding.markersCard.visibility = View.GONE
        }

        hideStyles()
        hideLayers()
    }

    private fun typeClick(type: Int) {
        val map = mMap ?: return
        setMapType(map, type) {
            hideLayers()
            if (type == GoogleMap.MAP_TYPE_NORMAL) {
                showStyles()
            }
        }
    }

    private fun styleClick(style: Int) {
        prefs.mapStyle = style
        val map = mMap ?: return
        refreshStyles(map)
        hideStyles()
    }

    fun invokeBack() {
        mListener?.onBackClick()
    }

    @SuppressLint("MissingPermission")
    private fun setMyLocation() {
        val context = activity ?: return
        if (Permissions.ensurePermissions(context, 205, Permissions.ACCESS_COARSE_LOCATION, Permissions.ACCESS_FINE_LOCATION)) {
            mMap?.isMyLocationEnabled = true
        }
    }

    private fun showPlaces(places: List<Place>) {
        placeRecyclerAdapter.actionsListener = object : ActionsListener<Place> {
            override fun onAction(view: View, position: Int, t: Place?, actions: ListActions) {
                when (actions) {
                    ListActions.OPEN, ListActions.MORE -> {
                        hideLayers()
                        if (t != null) {
                            addMarker(LatLng(t.latitude, t.longitude), markerTitle, true, t.markerColor, true, markerRadius)
                        }
                    }
                    else -> {
                    }
                }
            }
        }
        placeRecyclerAdapter.data = places
        if (places.isEmpty()) {
            binding.placesListCard.visibility = View.GONE
        } else {
            binding.placesListCard.visibility = View.VISIBLE
        }
    }

    private fun toggleMarkers() {
        if (isLayersVisible) {
            hideLayers()
        }
        if (isStylesVisible) {
            hideStyles()
        }
        showStyleDialog()
    }

    private fun toggleRadius() {
        if (isLayersVisible) {
            hideLayers()
        }
        if (isStylesVisible) {
            hideStyles()
        }
        showRadiusDialog()
    }

    private fun showStyles() {
        binding.mapStyleContainer.visibility = View.VISIBLE
    }

    private fun toggleLayers() {
        when {
            isLayersVisible -> hideLayers()
            isStylesVisible -> hideStyles()
            else -> binding.layersContainer.visibility = View.VISIBLE
        }
    }

    private fun hideLayers() {
        if (isLayersVisible) {
            binding.layersContainer.visibility = View.GONE
        }
    }

    private fun hideStyles() {
        if (isStylesVisible) {
            binding.mapStyleContainer.visibility = View.GONE
        }
    }

    private fun setStyle(map: GoogleMap, mapType: Int = mMapType) {
        val same = mMapType == mapType
        mMapType = mapType
        if (same) {
            if (mapType == GoogleMap.MAP_TYPE_NORMAL) {
                val ctx = context ?: return
                map.setMapStyle(MapStyleOptions.loadRawResourceStyle(ctx, mapStyleJson))
            }
        } else {
            if (mapType == GoogleMap.MAP_TYPE_NORMAL) {
                if (map.mapType == GoogleMap.MAP_TYPE_SATELLITE || map.mapType == GoogleMap.MAP_TYPE_HYBRID) {
                    map.mapType = GoogleMap.MAP_TYPE_NONE
                }
                map.mapType = mapType
                val ctx = context ?: return
                map.setMapStyle(MapStyleOptions.loadRawResourceStyle(ctx, mapStyleJson))
            } else {
                map.mapType = mapType
            }
        }
    }

    private fun setMapType(map: GoogleMap, type: Int, function: (() -> Unit)?) {
        setStyle(map, type)
        prefs.mapType = type
        function?.invoke()
    }

    private fun refreshStyles(map: GoogleMap) {
        setStyle(map, mMapType)
    }

    private val mapStyleJson: Int
        @RawRes
        get() {
            return when (prefs.mapStyle) {
                0 -> R.raw.map_terrain_day
                1 -> R.raw.map_terrain_retro
                2 -> R.raw.map_terrain_silver
                3 -> R.raw.map_terrain_night
                4 -> R.raw.map_terrain_dark
                5 -> R.raw.map_terrain_aubergine
                else -> R.raw.map_terrain_day
            }
        }

    override fun onResume() {
        binding.mapView.onResume()
        super.onResume()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.mapView.onDestroy()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        binding.mapView.onStop()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQ_LOC -> if (Permissions.isAllGranted(grantResults)) {
                moveToMyLocation()
            }
            205 -> if (Permissions.isAllGranted(grantResults)) {
                setMyLocation()
            } else {
                Toast.makeText(context, R.string.cant_access_location_services, Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {

        private const val REQ_LOC = 1245

        const val ENABLE_TOUCH = "enable_touch"
        const val ENABLE_PLACES = "enable_places"
        const val ENABLE_SEARCH = "enable_search"
        const val ENABLE_STYLES = "enable_styles"
        const val ENABLE_BACK = "enable_back"
        const val ENABLE_RADIUS = "enable_radius"
        const val MARKER_STYLE = "marker_style"

        fun newInstance(isTouch: Boolean, isPlaces: Boolean,
                        isSearch: Boolean, isStyles: Boolean,
                        isBack: Boolean): MapFragment {
            val fragment = MapFragment()
            val args = Bundle()
            args.putBoolean(ENABLE_TOUCH, isTouch)
            args.putBoolean(ENABLE_PLACES, isPlaces)
            args.putBoolean(ENABLE_SEARCH, isSearch)
            args.putBoolean(ENABLE_STYLES, isStyles)
            args.putBoolean(ENABLE_BACK, isBack)
            args.putBoolean(ENABLE_RADIUS, false)
            fragment.arguments = args
            return fragment
        }

        fun newInstance(isRadius: Boolean = true): MapFragment {
            val fragment = MapFragment()
            val args = Bundle()
            args.putBoolean(ENABLE_TOUCH, true)
            args.putBoolean(ENABLE_PLACES, true)
            args.putBoolean(ENABLE_STYLES, true)
            args.putBoolean(ENABLE_BACK, true)
            args.putBoolean(ENABLE_RADIUS, isRadius)
            fragment.arguments = args
            return fragment
        }
    }
}

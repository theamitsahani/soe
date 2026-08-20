package com.example.util

import org.json.JSONArray
import org.json.JSONObject

object MapHtmlBuilder {

    fun buildMarkersJson(items: List<SchoolMapItem>): String {
        val markersArray = JSONArray()
        for (item in items) {
            val obj = JSONObject().apply {
                put("id", item.schoolId)
                put("name", item.schoolName.replace("'", "\\'"))
                put("village", item.village.replace("'", "\\'"))
                put("block", item.block.replace("'", "\\'"))
                put("district", item.district.replace("'", "\\'"))
                put("lat", item.latitude)
                put("lng", item.longitude)
                put("status", item.status.name)
                put("principal", item.principalName.replace("'", "\\'"))
                put("mobile", item.principalMobile)
                put("empName", item.assignedEmployeeName.replace("'", "\\'"))
                put("compDate", item.completedDate)
                put("isExact", item.isExactCoordinate)
            }
            markersArray.put(obj)
        }
        return markersArray.toString()
    }

    fun buildMapHtml(
        items: List<SchoolMapItem>,
        userLocation: UserLocation? = null,
        initialLat: Double = 26.9124,
        initialLng: Double = 75.7873,
        initialZoom: Int = 8
    ): String {
        val markersArray = buildMarkersJson(items)

        val userObj = if (userLocation != null) {
            JSONObject().apply {
                put("lat", userLocation.latitude)
                put("lng", userLocation.longitude)
                put("accuracy", userLocation.accuracy)
            }.toString()
        } else "null"

        return """
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" integrity="sha256-p4NxAoJBhIIN+hmNHrzRCf9tD/miZyoHS5obTRR9BMY=" crossorigin="" />
    <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js" integrity="sha256-20nQCchB9co0qIjJZRGuk2/Z9VM+kNiyxNV1lvTlZBo=" crossorigin=""></script>
    <style>
        html, body, #map {
            margin: 0;
            padding: 0;
            width: 100%;
            height: 100%;
            background: #f1f5f9;
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
        }
        .custom-pin {
            display: flex;
            align-items: center;
            justify-content: center;
            cursor: pointer;
            transition: transform 0.2s cubic-bezier(0.34, 1.56, 0.64, 1);
        }
        .custom-pin:active, .custom-pin.selected {
            transform: scale(1.3) translateY(-4px);
            z-index: 1000 !important;
        }
        .pin-blob {
            width: 28px;
            height: 28px;
            border-radius: 50% 50% 50% 0;
            transform: rotate(-45deg);
            display: flex;
            align-items: center;
            justify-content: center;
            box-shadow: 0 4px 10px rgba(0,0,0,0.3);
            border: 2px solid #ffffff;
        }
        .pin-blob.green { background: #10B981; }
        .pin-blob.amber { background: #F59E0B; }
        .pin-blob.red   { background: #EF4444; }
        .pin-inner {
            transform: rotate(45deg);
            color: #ffffff;
            font-size: 13px;
            font-weight: bold;
            display: flex;
            align-items: center;
            justify-content: center;
        }
        .user-pulse {
            width: 18px;
            height: 18px;
            background: #3B82F6;
            border: 3px solid #ffffff;
            border-radius: 50%;
            box-shadow: 0 0 0 0 rgba(59, 130, 246, 0.7);
            animation: pulse 1.8s infinite;
        }
        @keyframes pulse {
            0% {
                transform: scale(0.95);
                box-shadow: 0 0 0 0 rgba(59, 130, 246, 0.7);
            }
            70% {
                transform: scale(1.1);
                box-shadow: 0 0 0 16px rgba(59, 130, 246, 0);
            }
            100% {
                transform: scale(0.95);
                box-shadow: 0 0 0 0 rgba(59, 130, 246, 0);
            }
        }
        .leaflet-control-zoom {
            border: none !important;
            box-shadow: 0 4px 12px rgba(0,0,0,0.15) !important;
            border-radius: 12px !important;
            overflow: hidden;
        }
        .leaflet-control-zoom a {
            background: #ffffff !important;
            color: #1e293b !important;
            font-size: 16px !important;
            width: 36px !important;
            height: 36px !important;
            line-height: 36px !important;
        }
        .map-badge {
            position: absolute;
            bottom: 12px;
            left: 12px;
            z-index: 500;
            background: rgba(15, 23, 42, 0.85);
            color: #f8fafc;
            padding: 4px 10px;
            border-radius: 8px;
            font-size: 11px;
            font-weight: 600;
            backdrop-filter: blur(4px);
            pointer-events: none;
        }
    </style>
</head>
<body>
    <div id="map"></div>
    <div id="school-count" class="map-badge">Loading map...</div>

    <script>
        var mapData = $markersArray;
        var initialUserLoc = $userObj;
        var map;
        var markersLayer = L.layerGroup();
        var userMarker = null;
        var userAccuracyCircle = null;
        var allMarkers = {};

        // Base tile layers
        var osmLayer = L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            maxZoom: 19,
            attribution: '© OpenStreetMap'
        });

        var satelliteLayer = L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}', {
            maxZoom: 19,
            attribution: '© Esri Satellite'
        });

        // Initialize Leaflet
        map = L.map('map', {
            center: [$initialLat, $initialLng],
            zoom: $initialZoom,
            zoomControl: false,
            layers: [osmLayer]
        });

        L.control.zoom({ position: 'bottomright' }).addTo(map);
        markersLayer.addTo(map);

        function createPinIcon(status) {
            var colorClass = 'red';
            var iconChar = '•';
            if (status === 'SUBMITTED' || status === 'REVIEWED') {
                colorClass = 'green';
                iconChar = '✓';
            } else if (status === 'ASSIGNED') {
                colorClass = 'amber';
                iconChar = '⏱';
            }

            var html = '<div class="custom-pin"><div class="pin-blob ' + colorClass + '"><div class="pin-inner">' + iconChar + '</div></div></div>';
            return L.divIcon({
                className: '',
                html: html,
                iconSize: [28, 28],
                iconAnchor: [14, 28]
            });
        }

        function renderMarkers(items) {
            markersLayer.clearLayers();
            allMarkers = {};

            var bounds = [];
            for (var i = 0; i < items.length; i++) {
                var it = items[i];
                if (!it.lat || !it.lng) continue;

                var marker = L.marker([it.lat, it.lng], {
                    icon: createPinIcon(it.status)
                });

                (function(item, m) {
                    m.on('click', function(e) {
                        L.DomEvent.stopPropagation(e);
                        selectMarker(item.id);
                        if (window.AndroidBridge && window.AndroidBridge.onSchoolClick) {
                            window.AndroidBridge.onSchoolClick(item.id);
                        }
                    });
                })(it, marker);

                markersLayer.addLayer(marker);
                allMarkers[it.id] = { marker: marker, data: it };
                bounds.push([it.lat, it.lng]);
            }

            document.getElementById('school-count').innerText = items.length + ' Schools on Map';

            if (bounds.length > 0 && !initialUserLoc) {
                map.fitBounds(bounds, { padding: [40, 40], maxZoom: 15 });
            }
        }

        function selectMarker(schoolId) {
            for (var id in allMarkers) {
                var el = allMarkers[id].marker.getElement();
                if (el) {
                    var pin = el.querySelector('.custom-pin');
                    if (pin) pin.classList.remove('selected');
                }
            }
            if (allMarkers[schoolId]) {
                var targetMarker = allMarkers[schoolId].marker;
                var el = targetMarker.getElement();
                if (el) {
                    var pin = el.querySelector('.custom-pin');
                    if (pin) pin.classList.add('selected');
                }
                map.panTo(targetMarker.getLatLng(), { animate: true, duration: 0.6 });
            }
        }

        function updateUserLocation(lat, lng, accuracy) {
            if (userMarker) {
                userMarker.setLatLng([lat, lng]);
            } else {
                var userIcon = L.divIcon({
                    className: '',
                    html: '<div class="user-pulse"></div>',
                    iconSize: [18, 18],
                    iconAnchor: [9, 9]
                });
                userMarker = L.marker([lat, lng], { icon: userIcon, zIndexOffset: 2000 }).addTo(map);
            }

            if (accuracy && accuracy > 0) {
                if (userAccuracyCircle) {
                    userAccuracyCircle.setLatLng([lat, lng]);
                    userAccuracyCircle.setRadius(accuracy);
                } else {
                    userAccuracyCircle = L.circle([lat, lng], {
                        radius: accuracy,
                        color: '#3B82F6',
                        weight: 1,
                        fillColor: '#93C5FD',
                        fillOpacity: 0.15
                    }).addTo(map);
                }
            }
        }

        function centerOnUser(lat, lng, zoom) {
            updateUserLocation(lat, lng);
            map.flyTo([lat, lng], zoom || 14, { animate: true, duration: 1 });
        }

        function centerOnSchool(schoolId) {
            if (allMarkers[schoolId]) {
                var it = allMarkers[schoolId].data;
                selectMarker(schoolId);
                map.flyTo([it.lat, it.lng], 16, { animate: true, duration: 0.8 });
            }
        }

        function filterMarkers(statusFilter, query, districtFilter, blockFilter) {
            var filtered = [];
            var q = (query || '').toLowerCase().trim();
            var d = (districtFilter || 'All').toLowerCase().trim();
            var b = (blockFilter || 'All').toLowerCase().trim();

            for (var i = 0; i < mapData.length; i++) {
                var item = mapData[i];
                
                // Status check
                if (statusFilter && statusFilter !== 'ALL') {
                    if (statusFilter === 'COMPLETED' && item.status !== 'SUBMITTED' && item.status !== 'REVIEWED') continue;
                    if (statusFilter === 'ASSIGNED' && item.status !== 'ASSIGNED') continue;
                    if (statusFilter === 'PENDING' && item.status !== 'PENDING') continue;
                }

                // District / Block check
                if (d !== 'all' && item.district.toLowerCase() !== d) continue;
                if (b !== 'all' && item.block.toLowerCase() !== b) continue;

                // Query check
                if (q.length > 0) {
                    var matchName = item.name.toLowerCase().indexOf(q) !== -1;
                    var matchVill = item.village.toLowerCase().indexOf(q) !== -1;
                    var matchBlock = item.block.toLowerCase().indexOf(q) !== -1;
                    var matchDist = item.district.toLowerCase().indexOf(q) !== -1;
                    var matchPrinc = item.principal.toLowerCase().indexOf(q) !== -1;
                    if (!matchName && !matchVill && !matchBlock && !matchDist && !matchPrinc) continue;
                }

                filtered.push(item);
            }
            renderMarkers(filtered);
        }

        function resetMapBounds() {
            var bounds = [];
            for (var id in allMarkers) {
                bounds.push(allMarkers[id].marker.getLatLng());
            }
            if (bounds.length > 0) {
                map.fitBounds(bounds, { padding: [40, 40], maxZoom: 15 });
            }
        }

        function updateMapData(newItems) {
            mapData = newItems;
            renderMarkers(mapData);
        }

        function switchLayer(layerType) {
            if (layerType === 'satellite') {
                map.removeLayer(osmLayer);
                satelliteLayer.addTo(map);
            } else {
                map.removeLayer(satelliteLayer);
                osmLayer.addTo(map);
            }
        }

        // Initial setup
        renderMarkers(mapData);

        if (initialUserLoc) {
            updateUserLocation(initialUserLoc.lat, initialUserLoc.lng, initialUserLoc.accuracy);
        }

        // Signal back to Android
        if (window.AndroidBridge && window.AndroidBridge.onMapReady) {
            window.AndroidBridge.onMapReady();
        }
    </script>
</body>
</html>
        """.trimIndent()
    }
}

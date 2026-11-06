package com.drivelog.mobile.core.model

import org.json.JSONObject

fun CardDraft.toSettings(): JSONObject = JSONObject()
    .put("template", layout.name).put("tone", filter.tone)
    .put("showRoute", showRoute).put("showDistance", showDistance).put("showTime", showTime)
    .put("showDate", showDate).put("showPlace", showPlace).put("showEfficiency", showEfficiency)
    .put("trimEnds", trimEnds)
    .put("crop", JSONObject().put("zoom", zoom).put("x", panX).put("y", panY))

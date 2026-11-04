package com.drivelog.mobile.data

object ArrivalPolicy {
    fun unseen(previous: Set<String>?, records: List<Pair<String, Boolean>>): List<String> =
        if (previous == null) emptyList() else records.filter { it.first !in previous && !it.second }.map { it.first }.distinct()
}

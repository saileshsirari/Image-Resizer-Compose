/*
 * SPDX-FileCopyrightText: 2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.image.resizer.compose.mediaApi.model

import android.database.Cursor
import com.image.resizer.compose.mediaApi.util.mapEachRow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

fun <T> Flow<Cursor?>.mapEachRow(
    projection: Array<String>,
    mapping: (Cursor, Array<Int>) -> T,
) = map { it.mapEachRow(projection, mapping) }
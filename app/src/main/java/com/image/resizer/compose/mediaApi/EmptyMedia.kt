/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.image.resizer.compose.mediaApi

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.image.resizer.compose.R

@Composable
fun EmptyMedia(
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.no_media_title),
) = LoadingMedia(
    modifier = modifier,
    shouldShimmer = false,
    bottomContent = {
        Text(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            text = title,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
    }
)
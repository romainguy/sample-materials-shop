/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.curiouscreature.compose.sample.shop

import androidx.compose.material.MaterialTheme
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val StoreColors = lightColors(
    primary = Color(0xffdd0d3e),
    onPrimary = Color.White,
    secondary = Color(0xfffbdf61),
    onSecondary = Color.Black,
    surface = Color(0xff060730),
    onSurface = Color.White
)

@Composable
fun StoreTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = StoreColors,
        content = content
    )
}

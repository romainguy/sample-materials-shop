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

import androidx.compose.*
import androidx.ui.tooling.preview.Preview
import com.curiouscreature.compose.sample.shop.AmountUnit.LITER

@Preview
@Composable
fun CartItemPreview() {
    StoreTheme {
        var quantity by state { 1 }
        var color by state { "Fiery Red" }

        ShoppingCartItem(
            Product(
                17,
                "Car paint",
                color,
                1_50,
                LITER,
                quantity
            ),
            increase = { quantity++ },
            decrease = { quantity-- },
            updateColor = {
                color =
                    nextProductColor(color)
            }
        ) {
            SampleImage(color)
        }
    }
}

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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.SurfaceView
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExtendedFloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.sharp.Add
import androidx.compose.material.icons.sharp.Remove
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import com.curiouscreature.compose.R
import com.google.android.filament.Colors
import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
import com.google.android.filament.IndirectLight
import com.google.android.filament.LightManager
import com.google.android.filament.Skybox
import com.google.android.filament.gltfio.AssetLoader
import com.google.android.filament.gltfio.ResourceLoader
import com.google.android.filament.gltfio.UbershaderProvider
import com.google.android.filament.utils.KTX1Loader
import com.google.android.filament.utils.Utils
import kotlin.collections.set

class MainActivity : ComponentActivity() {
    private lateinit var storeViewModel: StoreViewModel
    private lateinit var engine: Engine
    private lateinit var assetLoader: AssetLoader
    private lateinit var resourceLoader: ResourceLoader

    private lateinit var indirectLight: IndirectLight
    private lateinit var skybox: Skybox
    private var light: Int = 0

    companion object {
        init { Utils.init() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initFilament()

        storeViewModel = ViewModelProvider(this).get(StoreViewModel::class.java)
        val shoppingCart = storeViewModel.shoppingCart

        val increase: (Product) -> Unit = { product ->
            storeViewModel.update(product.copy(quantity = product.quantity + 1))
        }

        val decrease: (Product) -> Unit = { product ->
            if (product.quantity > 1) {
                storeViewModel.update(product.copy(quantity = product.quantity - 1))
            } else {
                storeViewModel.delete(product)
            }
        }

        val updateColor: (Product) -> Unit = { product ->
            storeViewModel.update(product.copy(color = nextProductColor(product.color)))
        }

        setContent {
            StoreTheme {
                Scaffold(
                    topBar = { StoreAppBar() },
                    floatingActionButton = { StoreCheckout(shoppingCart) }
                ) { padding ->
                    ShoppingCart(shoppingCart, increase, decrease, updateColor, padding)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        engine.lightManager.destroy(light)
        engine.destroyEntity(light)
        engine.destroyIndirectLight(indirectLight)
        engine.destroySkybox(skybox)

        scenes.forEach {
            engine.destroyScene(it.value.scene)
            assetLoader.destroyAsset(it.value.asset)
        }

        assetLoader.destroy()
        resourceLoader.destroy()

        engine.destroy()
    }

    private fun initFilament() {
        engine = Engine.create()
        assetLoader = AssetLoader(engine, UbershaderProvider(engine), EntityManager.get())
        resourceLoader = ResourceLoader(engine)

        val ibl = "courtyard_8k"
        readCompressedAsset(this, "envs/${ibl}/${ibl}_ibl.ktx").let {
            indirectLight = KTX1Loader.createIndirectLight(engine, it)
            indirectLight.intensity = 30_000.0f
        }

        readCompressedAsset(this, "envs/${ibl}/${ibl}_skybox.ktx").let {
            skybox = KTX1Loader.createSkybox(engine, it)
        }

        light = EntityManager.get().create()
        val (r, g, b) = Colors.cct(6_000.0f)
        LightManager.Builder(LightManager.Type.SUN)
                .color(r, g, b)
                .intensity(70_000.0f)
                .direction(0.28f, -0.6f, -0.76f)
                .build(engine, light)

        fun createScene(name: String, gltf: String) {
            val scene = engine.createScene()
            val asset = readCompressedAsset(this, gltf).let {
                val asset = loadModelGlb(assetLoader, resourceLoader, it)
                transformToUnitCube(engine, asset)
                asset
            }
            scene.indirectLight = indirectLight
            scene.skybox = skybox

            scene.addEntities(asset.entities)

            scene.addEntity(light)

            scenes[name] = ProductScene(engine, scene, asset)
        }

        createScene("Car paint", "models/car_paint/material_car_paint.glb")
        createScene("Carbon fiber", "models/carbon_fiber/material_carbon_fiber.glb")
        createScene("Lacquered wood", "models/lacquered_wood/material_lacquered_wood.glb")
        createScene("Wood", "models/wood/material_wood.glb")
    }
}

@Composable
fun ShoppingCart(
    shoppingCart: LiveData<List<Product>>,
    increase: (Product) -> Unit,
    decrease: (Product) -> Unit,
    updateColor: (Product) -> Unit,
    padding: PaddingValues
) {
    val products by shoppingCart.observeAsState(emptyList())
    LazyColumn {
        items(
            items = products,
            itemContent = { product ->
                ShoppingCartItem(product, increase, decrease, updateColor) {
                    FilamentViewer(product)
                }
            }
        )
    }
}

@Composable
fun ShoppingCartItem(
    product: Product,
    increase: (Product) -> Unit = {},
    decrease: (Product) -> Unit = {},
    updateColor: (Product) -> Unit = {},
    content: @Composable () -> Unit = {}
) {
    var selected by remember { mutableStateOf(false) }

    val topLeftCornerRadius by animateDpAsState(
        targetValue = if (selected) 48.dp else 8.dp,
        animationSpec = tween(durationMillis = 300)
    )

    val cornerRadius by animateDpAsState(
        targetValue = if (selected) 0.dp else 8.dp,
        animationSpec = tween(durationMillis = 300)
    )

    Surface(
        modifier = Modifier.padding(16.dp, 8.dp, 16.dp, 8.dp),
        shape = RoundedCornerShape(
            topStart = topLeftCornerRadius,
            topEnd = cornerRadius,
            bottomStart = cornerRadius,
            bottomEnd = cornerRadius
        ),
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier.toggleable(value = selected, onValueChange = { selected = it })
        ) {
            content()

            val selectedAlpha by animateFloatAsState(
                targetValue = if (selected) 0.65f else 0.0f,
                animationSpec = tween(durationMillis = 300)
            )

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colors.primary.copy(alpha = selectedAlpha)
            ) {
                Icon(
                    imageVector = Icons.Filled.Done,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = selectedAlpha)
                )
            }
        }

        ShoppingCartItemRow(product, decrease, increase, updateColor)
    }
}

@Composable
fun ShoppingCartItemRow(
    product: Product,
    decrease: (Product) -> Unit = {},
    increase: (Product) -> Unit = {},
    updateColor: (Product) -> Unit = {}
) {
    val hasColorSwatch = product.color.isProductColor
    ConstraintLayout(modifier = Modifier
        .padding(12.dp)
        .fillMaxWidth()) {
        val (decreaseRef, increaseRef, labelRef, colorRef, amountRef) = createRefs()

        SmallButton(
            modifier = Modifier.constrainAs(decreaseRef) {
                start.linkTo(parent.start)
                centerVerticallyTo(parent)
            },
            onClick = { decrease(product) }
        ) {
            Image(Icons.Sharp.Remove, contentDescription = null)
        }

        SmallButton(
            modifier = Modifier.constrainAs(increaseRef) {
                start.linkTo(decreaseRef.end, margin = 4.dp)
                centerVerticallyTo(parent)
            },
            onClick = { increase(product) }
        ) {
            Image(Icons.Sharp.Add, contentDescription = null)
        }

        Text(
            modifier = Modifier.constrainAs(labelRef) {
                start.linkTo(increaseRef.end, margin = 8.dp)
                centerVerticallyTo(parent)
            },
            text = "${product.quantity}× ${product.material}"
        )

        if (hasColorSwatch) {
            SmallButton(
                modifier = Modifier.constrainAs(colorRef) {
                    start.linkTo(labelRef.end, margin = 4.dp)
                    centerVerticallyTo(parent)
                },
                onClick = { updateColor(product) },
                color = Color.White
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(13.dp)
                        .background(color = productColor(product), shape = CircleShape)
                ) {}
            }
        }

        Text(
            modifier = Modifier.constrainAs(amountRef) {
                linkTo(
                    start = (if (hasColorSwatch) colorRef else labelRef).end,
                    end = parent.end,
                    bias = 1.0f
                )
                centerVerticallyTo(parent)
            },
            text = formatAmount(product)
        )
    }
}

@Composable
fun FilamentViewer(product: Product) {
    var modelViewer by remember { mutableStateOf<ModelViewer?>(null) }

    LaunchedEffect(true) {
        while (true) {
            withFrameNanos { nano ->
                modelViewer?.render(nano)
            }
        }
    }

    SideEffect {
        val (engine, scene, asset) = scenes[product.material]!!
        modelViewer?.scene = scene

        asset.entities.find {
            asset.getName(it)?.startsWith("car_paint_red") ?: false
        }?.also { entity ->
            val manager = engine.renderableManager
            val instance = manager.getInstance(entity)
            val material = manager.getMaterialInstanceAt(instance, 0)

            val productColor = productColor(product)

            val r = productColor.red
            val g = productColor.green
            val b = productColor.blue

            material.setParameter(
                "baseColorFactor", Colors.RgbaType.SRGB, r, g, b, 1.0f
            )
        }
    }

    AndroidView({ context ->
        FrameLayout(context).apply {
            LayoutInflater.from(context).inflate(
                R.layout.filament_host, this, true
            )

            val (engine) = scenes[product.material]!!
            modelViewer = ModelViewer(engine, getChildAt(0) as SurfaceView).also {
                setupModelViewer(it)
            }
        }
    })
}

@Composable
fun SmallButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = { },
    color: Color = MaterialTheme.colors.secondary,
    content: @Composable () -> Unit = { }
) {
    Surface(
        modifier = modifier.size(16.dp),
        color = color,
        shape = CircleShape,
        elevation = 2.dp
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.clickable(onClick = onClick)
        ) {
            content()
        }
    }
}

@Composable
fun StoreAppBar() {
    TopAppBar(title = { Text(stringResource(R.string.app_name)) })
}

@Composable
fun StoreCheckout(shoppingCart: LiveData<List<Product>>) {
    val products = shoppingCart.observeAsState(emptyList()).value
    ExtendedFloatingActionButton(
        text = { Text("${products.sumBy { it.quantity }} items") },
        icon = { Icon(Icons.Filled.ShoppingCart, contentDescription = null) },
        onClick = { }
    )
}

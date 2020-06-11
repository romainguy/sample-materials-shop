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
import android.view.Choreographer
import android.view.SurfaceView
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.*
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import androidx.ui.animation.animate
import androidx.ui.core.*
import androidx.ui.foundation.*
import androidx.ui.foundation.selection.Toggleable
import androidx.ui.foundation.shape.corner.CircleShape
import androidx.ui.foundation.shape.corner.RoundedCornerShape
import androidx.ui.graphics.Color
import androidx.ui.layout.*
import androidx.ui.layout.RowScope.gravity
import androidx.ui.livedata.observeAsState
import androidx.ui.material.*
import androidx.ui.material.icons.Icons
import androidx.ui.material.icons.filled.Done
import androidx.ui.material.icons.filled.ShoppingCart
import androidx.ui.material.icons.sharp.Add
import androidx.ui.material.icons.sharp.Remove
import androidx.ui.material.ripple.ripple
import androidx.ui.semantics.Semantics
import androidx.ui.unit.dp
import androidx.ui.viewinterop.AndroidView
import com.curiouscreature.compose.R
import com.google.android.filament.*
import com.google.android.filament.gltfio.AssetLoader
import com.google.android.filament.gltfio.MaterialProvider
import com.google.android.filament.gltfio.ResourceLoader
import com.google.android.filament.utils.KtxLoader
import com.google.android.filament.utils.Utils

class MainActivity : AppCompatActivity() {
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
                    topAppBar = { StoreAppBar() },
                    floatingActionButton = { StoreCheckout(shoppingCart) }
                ) { modifier ->
                    ShoppingCart(shoppingCart, increase, decrease, updateColor, modifier)
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
        assetLoader = AssetLoader(engine, MaterialProvider(engine), EntityManager.get())
        resourceLoader = ResourceLoader(engine)

        val ibl = "courtyard_8k"
        readCompressedAsset(this, "envs/${ibl}/${ibl}_ibl.ktx").let {
            indirectLight = KtxLoader.createIndirectLight(engine, it)
            indirectLight.intensity = 30_000.0f
        }

        readCompressedAsset(this, "envs/${ibl}/${ibl}_skybox.ktx").let {
            skybox = KtxLoader.createSkybox(engine, it)
        }

        light = EntityManager.get().create()
        val (r, g, b) = Colors.cct(6_000.0f)
        LightManager.Builder(LightManager.Type.SUN)
                .color(r, g, b)
                .intensity(70_000.0f)
                .direction(0.28f, -0.6f, -0.76f)
                //.castShadows(true)
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
    modifier: Modifier = Modifier
) {
    val products by shoppingCart.observeAsState(emptyList())
    AdapterList(
        modifier = modifier.padding(top = 8.dp),
        data = products
    ) { product ->
        ShoppingCartItem(product, increase, decrease, updateColor) {
            FilamentViewer(product)
        }
    }
}

@Composable
fun ShoppingCartItem(
    product: Product,
    increase: (Product) -> Unit = { },
    decrease: (Product) -> Unit = { },
    updateColor: (Product) -> Unit = { },
    content: @Composable() () -> Unit = { }
) {
    val (selected, onSelected) = state { false }

    val topLeftCornerRadius = animate(target = if (selected) 48.dp.value else 8.dp.value)
    val cornerRadius        = animate(target = if (selected)  0.dp.value else 8.dp.value)

    Surface(
        modifier = Modifier.padding(16.dp, 8.dp, 16.dp, 8.dp),
        shape = RoundedCornerShape(
            topLeft = topLeftCornerRadius.dp,
            topRight = cornerRadius.dp,
            bottomLeft = cornerRadius.dp,
            bottomRight = cornerRadius.dp
        ),
        elevation = 4.dp
    ) {
        Stack {
            Column {
                Toggleable(
                    value = selected,
                    onValueChange = onSelected,
                    modifier = Modifier.ripple()
                ) {
                    Stack {
                        content()

                        val selectedAlpha = animate(target = if (selected) 0.65f else 0.0f)
                        Surface(
                            modifier = Modifier.matchParentSize(),
                            color = MaterialTheme.colors.primary.copy(alpha = selectedAlpha)
                        ) {
                            Icon(
                                asset = Icons.Filled.Done,
                                tint = contentColor().copy(alpha = selectedAlpha)
                            )
                        }
                    }
                }

                ShoppingCartItemRow(product, decrease, increase, updateColor)
            }
        }
    }
}

private object IncreaseTag
private object DecreaseTag
private object LabelTag
private object ColorTag
private object AmountTag

@Composable
fun ShoppingCartItemRow(
    product: Product,
    decrease: (Product) -> Unit = { },
    increase: (Product) -> Unit = { },
    updateColor: (Product) -> Unit = { }
) {
    val hasColorSwatch = product.color.isProductColor
    ConstraintLayout(
        modifier = Modifier.padding(12.dp).fillMaxWidth(),
        constraintSet = ConstraintSet {
            val decreaseConstraint = tag(DecreaseTag).apply {
                left constrainTo parent.left
                centerVertically()
            }
            val increaseConstraint = tag(IncreaseTag).apply {
                left constrainTo decreaseConstraint.right
                left.margin = 4.dp
                centerVertically()
            }
            val labelConstraint = tag(LabelTag).apply {
                left constrainTo increaseConstraint.right
                left.margin = 8.dp
                centerVertically()
            }
            val colorConstraint = tag(ColorTag).apply {
                left constrainTo labelConstraint.right
                left.margin = 4.dp
                centerVertically()
            }
            tag(AmountTag).apply {
                left constrainTo (if (hasColorSwatch) colorConstraint else labelConstraint).right
                right constrainTo parent.right
                horizontalBias = 1.0f
                centerVertically()
            }
        }
    ) {
        SmallButton(
            modifier = Modifier.tag(DecreaseTag),
            onClick = { decrease(product) }
        ) {
            Image(Icons.Sharp.Remove)
        }

        SmallButton(
            modifier = Modifier.tag(IncreaseTag),
            onClick = { increase(product) }
        ) {
            Image(Icons.Sharp.Add)
        }

        Text(
            modifier = Modifier.tag(LabelTag),
            text = "${product.quantity}× ${product.material}"
        )

        if (hasColorSwatch) {
            SmallButton(
                modifier = Modifier.tag(ColorTag),
                onClick = { updateColor(product) },
                color = Color.White
            ) {
                Box(
                    modifier = Modifier.size(13.dp),
                    shape = CircleShape,
                    gravity = Alignment.Center,
                    backgroundColor = productColor(product)
                )
            }
        }

        Text(
            modifier = Modifier.tag(AmountTag),
            text = formatAmount(product)
        )
    }
}

@Composable
fun FilamentViewer(product: Product) {
    var modelViewer by state<ModelViewer?> { null }

    onActive {
        val frameCallback = object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                Choreographer.getInstance().postFrameCallback(this)
                modelViewer?.render(frameTimeNanos)
            }
        }

        Choreographer.getInstance().postFrameCallback(frameCallback)

        onDispose {
            Choreographer.getInstance().removeFrameCallback(frameCallback)
        }
    }

    onCommit(product) {
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

    AndroidView(R.layout.filament_host) { view ->
        val (engine) = scenes[product.material]!!
        modelViewer = ModelViewer(engine, view as SurfaceView).also {
            setupModelViewer(it)
        }
    }
}

@Composable
fun SmallButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = { },
    color: Color = MaterialTheme.colors.secondary,
    content: @Composable() () -> Unit = { }
) {
    Surface(
        modifier = modifier.size(16.dp).gravity(Alignment.CenterVertically),
        color = color,
        shape = CircleShape,
        elevation = 2.dp
    ) {
        Box(
            modifier = Modifier.clickable(onClick = onClick),
            gravity = Alignment.Center
        ) {
            content()
        }
    }
}

@Composable
fun StoreAppBar() {
    TopAppBar(title = { Text(ContextAmbient.current.getString(R.string.app_name)) })
}

@Composable
fun StoreCheckout(shoppingCart: LiveData<List<Product>>) {
    val products = shoppingCart.observeAsState(emptyList()).value
    ExtendedFloatingActionButton(
        text = { Text("${products.sumBy { it.quantity }} items") },
        icon = { Icon(Icons.Filled.ShoppingCart) },
        onClick = { }
    )
}

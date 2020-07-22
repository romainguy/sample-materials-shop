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

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

enum class AmountUnit(val shortName: String) {
    KILOGRAM("kg"),
    LITER("l");

    override fun toString() = shortName
}

@Entity(tableName = "shopping_cart")
data class Product(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val material: String,
    val color: String,
    val amount: Int,
    val unit: AmountUnit,
    val quantity: Int
)

@Dao
interface ShoppingCartDao {
    @Query("select * from shopping_cart")
    fun getProducts(): LiveData<List<Product>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(vararg product: Product)

    @Update
    suspend fun update(product: Product)

    @Delete
    suspend fun delete(product: Product)

    @Query("delete from shopping_cart")
    suspend fun deleteAll()
}

class StoreConverters {
    companion object{
        @TypeConverter
        @JvmStatic
        fun fromAmountUnit(value: AmountUnit) = value.toString()

        @TypeConverter
        @JvmStatic
        fun toAmountUnit(value: String) = AmountUnit.values().find { it.shortName == value }
    }
}

@Database(entities = [Product::class], version = 8, exportSchema = false)
@TypeConverters(StoreConverters::class)
abstract class StoreDatabase : RoomDatabase() {
    abstract fun shoppingCartDao(): ShoppingCartDao

    private class StoreDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {

        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            INSTANCE?.let { database ->
                scope.launch {
                    populateDatabase(database.shoppingCartDao())
                }
            }
        }

        suspend fun populateDatabase(shoppingCartDao: ShoppingCartDao) {
            // NOTE: This is a demo and to guarantee that every run starts in a good state,
            //       we reset the database completely here
            shoppingCartDao.deleteAll()

            shoppingCartDao.insert(
                Product(
                    0,
                    "Car paint",
                    "Fiery Red",
                    1_50,
                    AmountUnit.LITER,
                    3
                ),
                Product(
                    0,
                    "Wood",
                    "N/A",
                    10_00,
                    AmountUnit.KILOGRAM,
                    2
                ),
                Product(
                    0,
                    "Carbon fiber",
                    "N/A",
                    1_00,
                    AmountUnit.KILOGRAM,
                    1
                ),
                Product(
                    0,
                    "Lacquered wood",
                    "N/A",
                    12_00,
                    AmountUnit.KILOGRAM,
                    1
                )
            )
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: StoreDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): StoreDatabase {
            INSTANCE?.also { return@getDatabase INSTANCE!! }

            synchronized(this) {
                val instance = Room
                    .databaseBuilder(
                        context.applicationContext,
                        StoreDatabase::class.java,
                        "store_database"
                    )
                    .addCallback(StoreDatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                return instance
            }
        }
    }
}

class StoreViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ShoppingCartDao =
            StoreDatabase.getDatabase(application, viewModelScope).shoppingCartDao()
    val shoppingCart: LiveData<List<Product>>

    init {
        shoppingCart = repository.getProducts()
    }

    fun delete(product: Product) = viewModelScope.launch(Dispatchers.IO) {
        repository.delete(product)
    }

    fun update(product: Product) = viewModelScope.launch(Dispatchers.IO) {
        repository.update(product)
    }
}

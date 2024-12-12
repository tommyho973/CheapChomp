package com.example.cheapchomp.repository

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.cheapchomp.network.models.ProductPrice
import com.example.cheapchomp.ui.state.KrogerProductUiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

// offline database
@Entity(tableName = "item")
data class CachedGroceryItem(
    @PrimaryKey val id: Int,
    val item_id: String,
    val user_id: String,
    val name: String,
    val price: String,
    var favorited: Boolean,
    val storeId: String
)

@Dao
interface ItemsDao {
    @Insert
    fun insertItems(vararg items: CachedGroceryItem)

    @Query("SELECT * FROM item WHERE user_id = :userId")
    fun getAll(userId: String): List<CachedGroceryItem>

    @Query("SELECT * FROM item WHERE user_id = :userId AND name = :name")
    fun getItem(userId: String, name: String): CachedGroceryItem?

    @Query("SELECT * FROM item WHERE user_id = :userId AND favorited = 1")
    fun getFavoriteItems(userId: String): List<CachedGroceryItem>

    @Query("SELECT * FROM item WHERE user_id = :userId AND favorited = 0")
    fun getNonFavoriteItems(userId: String): List<CachedGroceryItem>

    @Delete
    fun delete(item: CachedGroceryItem)

    @Delete
    fun deleteAll(items: List<CachedGroceryItem>)
}

@Database(entities = [CachedGroceryItem::class], version = 3)
abstract class OfflineDatabase : RoomDatabase() {
    abstract fun itemsDao(): ItemsDao
}

class DatabaseRepository {
    private val db = Firebase.firestore


    @RequiresApi(Build.VERSION_CODES.O)
    fun addToDatabase(
        product: ProductPrice,
        storeId: String,
        quantity: Int,
        onSuccess: () -> Unit = {},
        onError: (Exception) -> Unit = {},
        room_db: OfflineDatabase
    ) {
        // initialize dao for offline database
        val itemsDao = room_db.itemsDao()

        getGroceryList { listRef ->
            Log.d("DATABASE", "Adding quantity: $quantity")

            db.collection("items")
                .whereEqualTo("grocery_list", listRef)
                .whereEqualTo("store_id", storeId)
                .whereEqualTo("name", product.name)
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        val doc = documents.documents[0]
                        val currentQuantity = doc.getLong("quantity")?.toInt() ?: 0
                        val newQuantity = currentQuantity + quantity

                        doc.reference.update("quantity", newQuantity)
                            .addOnSuccessListener {
                                Log.d("DATABASE", "Updated quantity: $newQuantity")
                                onSuccess()
                            }
                            .addOnFailureListener { e ->
                                Log.w("DATABASE", "Error updating document", e)
                                onError(e)
                            }
                    } else {
                        val item = hashMapOf(
                            "store_id" to storeId,
                            "item_id" to 0,
                            "name" to product.name,
                            "price" to product.price,
                            "quantity" to quantity,
                            "favorited" to false,
                            "date_added" to Instant.now(),
                            "grocery_list" to listRef
                        )

                        db.collection("items")
                            .add(item)
                            .addOnSuccessListener {
                                Log.d("DATABASE", "Added new item")
                                onSuccess()
                            }
                            .addOnFailureListener { e ->
                                Log.w("DATABASE", "Error adding document", e)
                                onError(e)
                            }
                        getUserRef { userRef ->
                            var cached = false
                            val items = itemsDao.getAll(userRef.id)
                            for (i in items) {
                                if (i.name == product.name) { // should change this to be by unique id
                                    cached = true
                                    break
                                }
                            }

                            if (!cached) {
                                val cachedItem = CachedGroceryItem(
                                    id = items.size,
                                    item_id = "0",
                                    user_id = userRef.id,
                                    name = product.name,
                                    price = product.price,
                                    favorited = false,
                                    storeId = storeId
                                )
                                itemsDao.insertItems(cachedItem)
                                Log.d("DATABASE", "Inserted item into room database: $cachedItem")

                            }

                        }

                    }
                }
                .addOnFailureListener { e ->
                    Log.w("DATABASE", "Error checking for existing item", e)
                    onError(e)
                }
        }
    }

    fun deleteFromDatabase(
        product: ProductPrice,
        storeId: String,
        onSuccess: () -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        getGroceryList { listRef ->
            db.collection("items")
                .whereEqualTo("grocery_list", listRef)
                .whereEqualTo("store_id", storeId)
                .whereEqualTo("name", product.name)
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        documents.documents[0].reference.delete()
                            .addOnSuccessListener {
                                Log.d("DATABASE", "Item deleted successfully")
                                onSuccess()
                            }
                            .addOnFailureListener { e ->
                                Log.w("DATABASE", "Error deleting document", e)
                                onError(e)
                            }
                    } else {
                        Log.d("DATABASE", "Item not found in database")
                        onError(Exception("Item not found"))
                    }
                }
                .addOnFailureListener { e ->
                    Log.w("DATABASE", "Error checking for existing item", e)
                    onError(e)
                }
        }
    }

    fun getUserRef(onResult: (DocumentReference) -> Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val email = currentUser?.email

        if (email == null) {
            return
        }

        db.collection("users")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val userId = querySnapshot.documents[0].reference
                    onResult(userId)
                }
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreError", "Error fetching user ID", e)
            }
    }

    fun getGroceryList(onResult: (DocumentReference) -> Unit) {
        getUserRef { userRef ->
            Log.d("Firestore", "User ID: ${userRef.id}")
            Log.d("Firestore", "User Ref Path: ${userRef.path}")

            db.collection("grocery_list")
                .whereEqualTo("user", userRef)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (!querySnapshot.isEmpty) {
                        val groceryListRef = querySnapshot.documents[0].reference
                        Log.d("Firestore", "GroceryList ID: ${groceryListRef.id}")
                        onResult(groceryListRef)
                    } else {
                        Log.d("Firestore", "No grocery list found")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("FirestoreError", "Error fetching grocery list ID", e)
                }
        }
    }

    fun getAllItems(listRef: DocumentReference, onResult: (List<GroceryItem>) -> Unit) {
        db.collection("items")
            .whereEqualTo("grocery_list", listRef)
            .get()
            .addOnSuccessListener { snapshot ->
                val items = snapshot.documents.mapNotNull { doc ->
                    GroceryItem(
                        id = doc.id,
                        name = doc.getString("name") ?: return@mapNotNull null,
                        price = doc.getString("price") ?: return@mapNotNull null,
                        quantity = doc.getLong("quantity")?.toInt() ?: 0,
                        storeId = doc.getString("store_id") ?: return@mapNotNull null
                    )
                }
                onResult(items)
            }
            .addOnFailureListener {
                Log.e("DatabaseRepository", "Error getting items", it)
                onResult(emptyList())
            }
    }

    fun updatePrice(itemId: String, newPrice: String) {
        db.collection("items")
            .document(itemId)
            .update("price", newPrice)
            .addOnFailureListener {
                Log.e("DatabaseRepository", "Error updating price", it)
            }
    }

    data class GroceryItem(
        val id: String,
        val name: String,
        val price: String,
        val quantity: Int,
        val storeId: String
    )

    // room database methods
    suspend fun displayCachedProducts(room_db: OfflineDatabase): Result<List<ProductPrice>> {
        return suspendCoroutine { continuation ->
            val emptyProductList: List<ProductPrice> = emptyList()
            var toReturn = Result.success(emptyProductList)
            getUserRef { userRef ->
                val itemsDao = room_db.itemsDao()
                val roomItems = itemsDao.getAll(userRef.id)
                if (roomItems.isNotEmpty()) {
                    val products = roomItems.map { item ->
                        ProductPrice(
                            name = item.name,
                            price = item.price,
                            imageUrl = ""
                        )
                    }
                    Log.d("DATABASE", "Displaying cached products: $products")
                    toReturn = Result.success(products)
                }
                continuation.resume(toReturn)
            }
        }
    }

    fun clearCachedProducts(room_db: OfflineDatabase) {
        getUserRef { userRef ->
            val itemsDao = room_db.itemsDao()
            itemsDao.deleteAll(itemsDao.getNonFavoriteItems(userRef.id))
        }
    }

    suspend fun displayFavoriteProducts(room_db: OfflineDatabase): Result<List<ProductPrice>> {
        return suspendCoroutine { continuation ->
            val emptyProductList: List<ProductPrice> = emptyList()
            var toReturn = Result.success(emptyProductList)
            getUserRef { userRef ->
                val itemsDao = room_db.itemsDao()
                val roomItems = itemsDao.getFavoriteItems(userRef.id)
                if (roomItems.isNotEmpty()) {
                    val products = roomItems.map { item ->
                        ProductPrice(
                            name = item.name,
                            price = item.price,
                            imageUrl = ""
                        )
                    }
                    Log.d("DATABASE", "Displaying favorited products: $products")
                    toReturn = Result.success(products)
                }
                continuation.resume(toReturn)
            }
        }
    }

    fun addToFavorites(product: ProductPrice, storeId: String, room_db: OfflineDatabase) {
        getUserRef { userRef ->
            val itemsDao = room_db.itemsDao()
            var cachedItem = itemsDao.getItem(userRef.id, product.name)
            if (cachedItem != null) {
                itemsDao.delete(cachedItem)
                cachedItem.favorited = true
                itemsDao.insertItems(cachedItem)
            } else {
                val items = itemsDao.getAll(userRef.id)
                cachedItem = CachedGroceryItem(
                    id = items.size,
                    item_id = "0",
                    user_id = userRef.id,
                    name = product.name,
                    price = product.price,
                    favorited = true,
                    storeId = storeId
                )
                itemsDao.insertItems(cachedItem)
                Log.d("DATABASE", "Inserted item into room database: $cachedItem")
            }
        }

    }

}



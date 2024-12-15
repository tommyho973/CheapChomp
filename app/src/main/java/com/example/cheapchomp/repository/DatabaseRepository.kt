package com.example.cheapchomp.repository

import android.content.Context
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
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.cheapchomp.MyApp
import com.example.cheapchomp.network.models.ProductPrice
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.firestore
import com.google.type.TimeZone
import java.time.Instant
import java.util.Calendar
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

// offline database
@Entity(tableName = "item")
data class CachedGroceryItem(
    @PrimaryKey val id: Int,
    val user_id: String,
    val name: String,
    val price: String,
    var favorited: Boolean,
    var inGroceryList: Boolean,
    val storeId: String,
    val lastModified: Long = System.currentTimeMillis(),
    var pendingSync: Boolean = true
)

@Dao
interface ItemsDao {
    @Insert
    fun insertItems(vararg items: CachedGroceryItem)

    @Query("SELECT * FROM item")
    fun getAll(): List<CachedGroceryItem>

    @Query("SELECT * FROM item WHERE user_id = :userId")
    fun getAll(userId: String): List<CachedGroceryItem>

    @Query("SELECT * FROM item WHERE user_id = :userId AND name = :name")
    fun getItem(userId: String, name: String): CachedGroceryItem?

    @Query("SELECT * FROM item WHERE user_id = :userId AND favorited = 1")
    fun getFavoriteItems(userId: String): List<CachedGroceryItem>

    @Query("SELECT * FROM item WHERE user_id = :userId AND inGroceryList = 1")
    fun getGroceryList(userId: String): List<CachedGroceryItem>

    @Query("SELECT * FROM item WHERE user_id = :userId AND favorited = 0 AND inGroceryList = 0")
    fun getNonFavoriteItems(userId: String): List<CachedGroceryItem> // also leaves out items in grocery list

    @Query("SELECT * FROM Item WHERE pendingSync = 1")
    fun getUnsyncedItems(): List<CachedGroceryItem>

    @Query("UPDATE Item SET pendingSync = :syncStatus WHERE id = :itemId")
    suspend fun updateSyncStatus(itemId: Int, syncStatus: Boolean)

    @Delete
    fun delete(item: CachedGroceryItem)

    @Delete
    fun deleteAll(items: List<CachedGroceryItem>)
}

@Database(entities = [CachedGroceryItem::class], version = 4)
abstract class OfflineDatabase : RoomDatabase() {
    abstract fun itemsDao(): ItemsDao
}

class SyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    val databaseRepository = DatabaseRepository()
    private val room_db: OfflineDatabase = (context.applicationContext as MyApp).roomDB
    val itemsDao = room_db.itemsDao()

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun doWork(): Result {
        val unsyncedItems = itemsDao.getUnsyncedItems()
        if (unsyncedItems.isEmpty()) return Result.success()

        try {
            for (item in unsyncedItems) {
                if (item.inGroceryList) {
                    // add to Firestore grocery list
                    databaseRepository.getGroceryList { listRef ->
                        databaseRepository.getAllItems(listRef) { items ->
                            if (items.any { it.name == item.name }) {
                                // do nothing
                            } else {
                                val product = ProductPrice(
                                    name = item.name,
                                    price = item.price,
                                    imageUrl = ""
                                )
                                databaseRepository.addToDatabase(product, item.storeId, 1, room_db = room_db)

                            }
                        }
                    }
                } else {
                    // Remove from Firestore grocery list
                    val product = ProductPrice(
                        name = item.name,
                        price = item.price,
                        imageUrl = ""
                    )
                    databaseRepository.deleteFromDatabase(product, item.storeId)
                }

                // Mark as synced
                itemsDao.updateSyncStatus(item.id, false)
            }

            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.retry() // Retry syncing in case of failure
        }
    }
}


class DatabaseRepository() {
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
                        addToOfflineDatabase(product, room_db, storeId)
                        addToOfflineGroceryList(product, storeId, room_db)
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
                        addToOfflineDatabase(product, room_db, storeId)
                        addToOfflineGroceryList(product, storeId, room_db)
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
    suspend fun displayCachedProducts(room_db: OfflineDatabase, display: String): Result<List<ProductPrice>> {
        return suspendCoroutine { continuation ->
            val emptyProductList: List<ProductPrice> = emptyList()
            var toReturn = Result.success(emptyProductList)
            getUserRef { userRef ->
                val itemsDao = room_db.itemsDao()
                val roomItems = if (display == "favorites") {
                    itemsDao.getFavoriteItems(userRef.id)
                } else {
                    itemsDao.getAll(userRef.id)
                }
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


    suspend fun displayOfflineGroceryList(room_db: OfflineDatabase): Result<List<GroceryItem>> {
        return suspendCoroutine { continuation ->
            val emptyProductList: List<GroceryItem> = emptyList()
            var toReturn = Result.success(emptyProductList)
            getUserRef { userRef ->
                val itemsDao = room_db.itemsDao()
                val roomItems = itemsDao.getGroceryList(userRef.id)
                if (roomItems.isNotEmpty()) {
                    val products = roomItems.map { item ->
                        GroceryItem(
                            id = item.id.toString(),
                            name = item.name,
                            price = item.price,
                            quantity = 1,
                            storeId = item.storeId
                        )
                    }
                    Log.d("DATABASE", "Displaying cached products: $products")
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
                var inGroceryList = false
                getGroceryList { listRef ->
                    getAllItems(listRef) { allItems ->
                        for (item in allItems) {
                            if (item.name == product.name) {
                                inGroceryList = true
                            }

                        }
                        cachedItem = CachedGroceryItem(
                            id = itemsDao.getAll().size,
                            user_id = userRef.id,
                            name = product.name,
                            price = product.price,
                            favorited = true,
                            inGroceryList = inGroceryList,
                            storeId = storeId
                        )
                        itemsDao.insertItems(cachedItem!!)
                        Log.d("DATABASE", "Inserted item into room database: $cachedItem")
                    }
                }
            }
        }
    }

    fun removeFromFavorites(product: ProductPrice, room_db: OfflineDatabase) {
        getUserRef { userRef ->
            val itemsDao = room_db.itemsDao()
            val cachedItem = itemsDao.getItem(userRef.id, product.name)
            if (cachedItem != null) {
                itemsDao.delete(cachedItem)
                cachedItem.favorited = false
                itemsDao.insertItems(cachedItem)
            }
        }
    }

    fun addToOfflineDatabase(product: ProductPrice, room_db: OfflineDatabase, storeId: String) {
        getUserRef { userRef ->
            val itemsDao = room_db.itemsDao()
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
                    id = itemsDao.getAll().size,
                    user_id = userRef.id,
                    name = product.name,
                    price = product.price,
                    favorited = false,
                    inGroceryList = false,
                    storeId = storeId
                )
                itemsDao.insertItems(cachedItem)
                Log.d("DATABASE", "Inserted item into room database: $cachedItem")


            }

        }
    }

    fun deleteFromOfflineDatabase(product: ProductPrice, room_db: OfflineDatabase) {
        getUserRef { userRef ->
            val itemsDao = room_db.itemsDao()
            val cachedItem = itemsDao.getItem(userRef.id, product.name)
            if (cachedItem != null) {
                itemsDao.delete(cachedItem)
            }
        }
    }

    fun addToOfflineGroceryList(product: ProductPrice, storeId: String, room_db: OfflineDatabase) {
        getUserRef { userRef ->
            val itemsDao = room_db.itemsDao()

            var cachedItem = itemsDao.getItem(userRef.id, product.name)
            if (cachedItem != null) {
                Log.d("DATABASE", "Adding item to offline grocery list, NOT null: $product")
                itemsDao.delete(cachedItem)
                cachedItem.inGroceryList = true
                cachedItem.pendingSync = true
                itemsDao.insertItems(cachedItem)
            } else {
                Log.d("DATABASE", "Adding item to offline grocery list, null: $product")
                itemsDao.getAll().size
                cachedItem = CachedGroceryItem(
                    id = itemsDao.getAll().size,
                    user_id = userRef.id,
                    name = product.name,
                    price = product.price,
                    favorited = false,
                    inGroceryList = true,
                    storeId = storeId,
                    pendingSync = true
                )
                itemsDao.insertItems(cachedItem)
                Log.d("DATABASE", "Inserted item into room database: $cachedItem")


            }
        }


    }

    fun removeFromOfflineGroceryList(name: String, room_db: OfflineDatabase) {
        getUserRef { userRef ->
            val itemsDao = room_db.itemsDao()
            val cachedItem = itemsDao.getItem(userRef.id, name)
            if (cachedItem != null) {
                itemsDao.delete(cachedItem)
                cachedItem.inGroceryList = false
                cachedItem.pendingSync = true
                itemsDao.insertItems(cachedItem)
            }
        }
    }

    fun addToExpenses(price: Float) {
        getUserRef { userRef ->
            val timestamp = System.currentTimeMillis()
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = timestamp
            val month = calendar.get(Calendar.MONTH) + 1

            db.collection("expenses")
                .whereEqualTo("user", userRef)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (!querySnapshot.isEmpty) {
                        val doc = querySnapshot.documents[0]
                        val currentExpenses = doc.getLong(month.toString())?.toInt() ?: 0
                        val newExpenses = currentExpenses + price

                        doc.reference.update(month.toString(), newExpenses)
                            .addOnSuccessListener {
                                Log.d("DATABASE", "Updated quantity: $newExpenses")
                            }
                            .addOnFailureListener { e ->
                                Log.w("DATABASE", "Error updating document", e)
                            }
                    } else {
                        Log.d("Firestore", "No expenses reference found")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("FirestoreError", "Error fetching grocery list ID", e)
                }
        }
    }

}



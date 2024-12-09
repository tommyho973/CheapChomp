package com.example.cheapchomp.repository

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.cheapchomp.network.models.ProductPrice
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.firestore
import java.time.Instant

class DatabaseRepository {
    private val db = Firebase.firestore

    @RequiresApi(Build.VERSION_CODES.O)
    fun addToDatabase(
        product: ProductPrice,
        storeId: String,
        quantity: Int,
        onSuccess: () -> Unit = {},
        onError: (Exception) -> Unit = {}
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
}



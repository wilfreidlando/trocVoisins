package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.SeedData
import com.example.data.local.TrocDatabase
import com.example.data.model.Category
import com.example.data.model.ChatMessageEntity
import com.example.data.model.CollectionPointEntity
import com.example.data.model.DisputeEntity
import com.example.data.model.ExchangeType
import com.example.data.model.FundraiserEntity
import com.example.data.model.ItemEntity
import com.example.data.model.ReviewEntity
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.example.data.repository.SmartAnalysisResult
import com.example.data.repository.TrocRepository
import com.example.firebase.getAppFirestore
import com.example.firebase.getFirebaseAuthSafe
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class Screen {
    HOME,
    DETAIL,
    PUBLISH,
    CHAT,
    PROFILE,
    LOCATION_SETTINGS,
    AUTH,
    FUNDRAISERS,
    COLLECTION_POINTS,
    ADMIN_MEDIATION
}

enum class ViewMode {
    LIST,
    MAP
}

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

class TrocViewModel(application: Application) : AndroidViewModel(application) {

    private val _themeMode = MutableStateFlow(ThemeMode.LIGHT)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
    }

    private val repository: TrocRepository
    private val auth: FirebaseAuth? = getFirebaseAuthSafe()

    private val _firebaseUser = MutableStateFlow<FirebaseUser?>(auth?.currentUser)
    val firebaseUser: StateFlow<FirebaseUser?> = _firebaseUser.asStateFlow()

    private val _isLocalLoggedIn = MutableStateFlow(true)
    val isUserLoggedIn: StateFlow<Boolean> = combine(_firebaseUser, _isLocalLoggedIn) { fbUser, local ->
        fbUser != null || local
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        _firebaseUser.value = user
        if (user != null) {
            viewModelScope.launch {
                repository.syncFirebaseUser(user)
            }
        }
    }

    init {
        auth?.addAuthStateListener(authStateListener)
        val database = TrocDatabase.getDatabase(application)
        val firestore = getAppFirestore(application)
        repository = TrocRepository(
            dao = database.trocDao(),
            firestore = firestore,
            context = application
        )
    }

    override fun onCleared() {
        super.onCleared()
        auth?.removeAuthStateListener(authStateListener)
    }

    // Navigation
    private val _currentScreen = MutableStateFlow(Screen.HOME)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _selectedItemId = MutableStateFlow<String?>(null)
    val selectedItemId: StateFlow<String?> = _selectedItemId.asStateFlow()

    // Filters & Search
    private val _selectedCategory = MutableStateFlow(Category.TOUT)
    val selectedCategory: StateFlow<Category> = _selectedCategory.asStateFlow()

    private val _exchangeFilter = MutableStateFlow<ExchangeType?>(null)
    val exchangeFilter: StateFlow<ExchangeType?> = _exchangeFilter.asStateFlow()

    private val _selectedCity = MutableStateFlow("Toutes les villes")
    val selectedCity: StateFlow<String> = _selectedCity.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _radiusKm = MutableStateFlow(3.0f)
    val radiusKm: StateFlow<Float> = _radiusKm.asStateFlow()

    private val _viewMode = MutableStateFlow(ViewMode.LIST)
    val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()

    // Map selected item
    private val _mapSelectedItem = MutableStateFlow<ItemEntity?>(null)
    val mapSelectedItem: StateFlow<ItemEntity?> = _mapSelectedItem.asStateFlow()

    // Filtered Items Flow
    @OptIn(ExperimentalCoroutinesApi::class)
    val items: StateFlow<List<ItemEntity>> = kotlinx.coroutines.flow.combine(
        _selectedCategory,
        _exchangeFilter,
        _searchQuery,
        _selectedCity,
        _radiusKm
    ) { cat, exch, query, city, rad ->
        FilterState(cat, exch, query, city, rad)
    }.flatMapLatest { state ->
        repository.getFilteredItems(state.cat, state.exch, state.query, state.city, state.rad)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allItems: StateFlow<List<ItemEntity>> = repository.getAllItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allItemsForAdmin: StateFlow<List<ItemEntity>> = repository.getAllItemsForAdmin()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reportedItems: StateFlow<List<ItemEntity>> = repository.getReportedItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Current Item
    @OptIn(ExperimentalCoroutinesApi::class)
    val currentItem: StateFlow<ItemEntity?> = _selectedItemId.flatMapLatest { id ->
        if (id != null) repository.getItem(id)
        else kotlinx.coroutines.flow.flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Fundraisers
    val fundraisers: StateFlow<List<FundraiserEntity>> = repository.getFundraisers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Collection Points
    val collectionPoints: StateFlow<List<CollectionPointEntity>> = repository.getCollectionPoints()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Disputes for Admin Mediation
    val disputes: StateFlow<List<DisputeEntity>> = repository.getDisputes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Chat messages
    @OptIn(ExperimentalCoroutinesApi::class)
    val chatMessages: StateFlow<List<ChatMessageEntity>> = _selectedItemId.flatMapLatest { id ->
        if (id != null) repository.getChatMessages(id)
        else kotlinx.coroutines.flow.flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Current User - Defaults to Admin
    val currentUser: StateFlow<UserEntity?> = repository.getCurrentUser()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SeedData.adminUser)

    // Reviews for user
    val reviews: StateFlow<List<ReviewEntity>> = repository.getReviews(SeedData.adminUser.id)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Smart Vision Analysis State
    private val _analysisResult = MutableStateFlow<SmartAnalysisResult?>(null)
    val analysisResult: StateFlow<SmartAnalysisResult?> = _analysisResult.asStateFlow()

    // Navigation
    fun navigateTo(screen: Screen) {
        if (screen == Screen.PROFILE && !isUserLoggedIn.value) {
            _currentScreen.value = Screen.AUTH
        } else if (screen == Screen.ADMIN_MEDIATION) {
            val user = currentUser.value
            if (user?.isAdmin == true || user?.role == "ADMIN_MEDIATEUR") {
                _currentScreen.value = screen
            }
        } else {
            _currentScreen.value = screen
        }
    }

    fun openItemDetail(itemId: String) {
        _selectedItemId.value = itemId
        _currentScreen.value = Screen.DETAIL
    }

    fun openChat(itemId: String) {
        _selectedItemId.value = itemId
        _currentScreen.value = Screen.CHAT
    }

    fun navigateBack() {
        when (_currentScreen.value) {
            Screen.DETAIL -> _currentScreen.value = Screen.HOME
            Screen.CHAT -> _currentScreen.value = Screen.DETAIL
            Screen.PUBLISH -> _currentScreen.value = Screen.HOME
            Screen.PROFILE -> _currentScreen.value = Screen.HOME
            Screen.LOCATION_SETTINGS -> _currentScreen.value = Screen.PROFILE
            Screen.AUTH -> _currentScreen.value = if (isUserLoggedIn.value) Screen.PROFILE else Screen.HOME
            Screen.FUNDRAISERS -> _currentScreen.value = Screen.HOME
            Screen.COLLECTION_POINTS -> _currentScreen.value = Screen.HOME
            Screen.ADMIN_MEDIATION -> _currentScreen.value = Screen.PROFILE
            Screen.HOME -> { /* at root */ }
        }
    }

    // Filter mutations
    fun setCategory(category: Category) {
        _selectedCategory.value = category
    }

    fun setExchangeFilter(type: ExchangeType?) {
        _exchangeFilter.value = type
    }

    fun setSelectedCity(city: String) {
        _selectedCity.value = city
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setRadiusKm(radius: Float) {
        _radiusKm.value = radius
    }

    fun setViewMode(mode: ViewMode) {
        _viewMode.value = mode
    }

    fun selectMapItem(item: ItemEntity?) {
        _mapSelectedItem.value = item
    }

    // Actions
    fun reserveCurrentItem() {
        val id = _selectedItemId.value ?: return
        viewModelScope.launch {
            repository.reserveItem(id)
        }
    }

    fun completeCurrentItemExchange() {
        val id = _selectedItemId.value ?: return
        viewModelScope.launch {
            repository.completeItemExchange(id)
        }
    }

    fun sendChatMessage(text: String, neighborName: String) {
        val id = _selectedItemId.value ?: return
        if (text.isBlank()) return
        viewModelScope.launch {
            repository.sendChatMessage(id, text.trim(), neighborName)
        }
    }

    fun analyzePhoto(hint: String, imageUri: String? = null) {
        viewModelScope.launch {
            val res = repository.analyzePhoto(hint, imageUri)
            _analysisResult.value = res
        }
    }

    fun clearAnalysisResult() {
        _analysisResult.value = null
    }

    fun publishItem(
        title: String,
        description: String,
        category: Category,
        condition: String,
        exchangeType: ExchangeType,
        wantedInReturn: String,
        imageUri: String,
        isEmergencyResponse: Boolean,
        targetOrphanageName: String,
        onSuccess: (String) -> Unit
    ) {
        viewModelScope.launch {
            val newId = repository.publishItem(
                title = title,
                description = description,
                category = category,
                condition = condition,
                exchangeType = exchangeType,
                wantedInReturn = wantedInReturn,
                imageUri = imageUri,
                isEmergencyResponse = isEmergencyResponse,
                targetOrphanageName = targetOrphanageName
            )
            _selectedItemId.value = newId
            _analysisResult.value = null
            onSuccess(newId)
        }
    }

    fun contributeToFundraiser(fundraiserId: String, amountFcfa: Long, grains: Int) {
        viewModelScope.launch {
            repository.contributeToFundraiser(fundraiserId, amountFcfa, grains)
        }
    }

    fun reportCurrentItem(reason: String, details: String) {
        val item = currentItem.value ?: return
        viewModelScope.launch {
            repository.reportItemToMediation(
                itemId = item.id,
                itemTitle = item.title,
                donorId = item.donorId,
                donorName = item.donorName,
                reason = reason,
                details = details
            )
        }
    }

    fun resolveDispute(disputeId: String, status: String, notes: String) {
        viewModelScope.launch {
            repository.resolveDispute(disputeId, status, notes)
        }
    }

    fun approveItem(itemId: String) {
        viewModelScope.launch {
            repository.approveItem(itemId)
        }
    }

    fun rejectItem(itemId: String) {
        viewModelScope.launch {
            repository.rejectItem(itemId)
        }
    }

    fun switchUserRole(role: UserRole) {
        viewModelScope.launch {
            repository.switchUserRole(role)
            val fbUser = auth?.currentUser
            if (fbUser != null) {
                repository.syncFirebaseUser(fbUser, role)
            }
        }
    }

    fun updateLocation(city: String, neighborhood: String, landmark: String, radius: Float) {
        viewModelScope.launch {
            repository.updateLocation(city, neighborhood, landmark, radius)
            _radiusKm.value = radius
        }
    }

    fun submitReview(targetUserId: String, rating: Int, badge: String, comment: String) {
        viewModelScope.launch {
            repository.addReview(targetUserId, rating, badge, comment)
        }
    }

    fun loginAsAdmin() {
        viewModelScope.launch {
            repository.resetDatabaseToAdminOnly()
            _isLocalLoggedIn.value = true
            _currentScreen.value = Screen.HOME
        }
    }

    fun loginWithEmail(email: String, role: UserRole? = null) {
        viewModelScope.launch {
            if (email.contains("admin", ignoreCase = true) || email.contains("mediation", ignoreCase = true)) {
                repository.switchUserRole(UserRole.ADMIN_MEDIATEUR)
            } else if (role != null) {
                repository.switchUserRole(role)
            }
            _isLocalLoggedIn.value = true
            _currentScreen.value = Screen.HOME
        }
    }

    fun resetDatabase() {
        viewModelScope.launch {
            repository.resetDatabaseToAdminOnly()
        }
    }

    fun signOut() {
        _isLocalLoggedIn.value = false
        auth?.signOut()
        _currentScreen.value = Screen.AUTH
    }

    fun proposeBarter(itemId: String, proofDescription: String, proofPhotoUri: String?) {
        viewModelScope.launch {
            repository.proposeBarter(itemId, proofDescription, proofPhotoUri)
        }
    }

    fun toggleThemeMode() {
        _themeMode.value = if (_themeMode.value == ThemeMode.LIGHT) ThemeMode.DARK else ThemeMode.LIGHT
    }
}

private data class FilterState(
    val cat: Category,
    val exch: ExchangeType?,
    val query: String,
    val city: String,
    val rad: Float
)

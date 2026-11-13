package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppNotification
import com.example.data.model.AssociationStatus
import com.example.data.model.Category
import com.example.data.model.ChatMessage
import com.example.data.model.ChatThread
import com.example.data.model.CollectionPoint
import com.example.data.model.Contribution
import com.example.data.model.Dispute
import com.example.data.model.DisputeAction
import com.example.data.model.DisputeReason
import com.example.data.model.ExchangeType
import com.example.data.model.Fundraiser
import com.example.data.model.FundraiserStatus
import com.example.data.model.GrainRules
import com.example.data.model.Item
import com.example.data.model.ItemRequest
import com.example.data.model.ItemStatus
import com.example.data.model.LedgerEntry
import com.example.data.model.LedgerReason
import com.example.data.model.NotificationType
import com.example.data.model.RequestStatus
import com.example.data.model.Review
import com.example.data.model.Cities
import com.example.data.model.UserProfile
import com.example.data.repository.AdminRepository
import com.example.data.repository.AiRepository
import com.example.data.repository.AiSuggestion
import com.example.data.repository.AuthRepository
import com.example.data.repository.ChatRepository
import com.example.data.repository.CollectionPointForm
import com.example.data.repository.FundraiserForm
import com.example.data.repository.FundraiserRepository
import com.example.data.repository.GrainsRepository
import com.example.data.repository.ItemRepository
import com.example.data.repository.NotificationRepository
import com.example.data.repository.OnboardingForm
import com.example.data.repository.PublishForm
import com.example.data.repository.SocialRepository
import com.example.data.repository.ThreadSeed
import com.example.firebase.getAppFirestore
import com.example.firebase.getFirebaseAuthSafe
import com.example.util.ImageUtils
import com.example.util.LocationUtils
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** État de la session. */
sealed interface Session {
    data object Loading : Session
    data object SignedOut : Session
    data class NeedsProfile(val user: FirebaseUser) : Session
    data class Active(val profile: UserProfile) : Session
}

data class HomeFilters(
    val category: Category = Category.TOUT,
    val exchange: ExchangeType? = null,
    val query: String = "",
    /** null = sans limite de distance */
    val radiusKm: Float? = 5f,
    val onlyEmergency: Boolean = false
)

enum class HomeViewMode { LIST, MAP }
enum class ThemeMode { LIGHT, DARK, SYSTEM }

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(application: Application) : AndroidViewModel(application) {

    // ------------------------------------------------------------------ Backend
    private val auth = getFirebaseAuthSafe()
    private val db = getAppFirestore(application)
    val backendAvailable: Boolean = auth != null && db != null

    private val chatRepo: ChatRepository?
    private val authRepo: AuthRepository?
    private val itemRepo: ItemRepository?
    private val grainsRepo: GrainsRepository?
    private val socialRepo: SocialRepository?
    private val notifRepo: NotificationRepository?
    private val fundRepo: FundraiserRepository?
    private val adminRepo: AdminRepository?
    private val aiRepo = AiRepository()

    init {
        if (auth != null && db != null) {
            chatRepo = ChatRepository(db)
            authRepo = AuthRepository(auth, db)
            itemRepo = ItemRepository(db, chatRepo)
            grainsRepo = GrainsRepository(db)
            socialRepo = SocialRepository(db)
            notifRepo = NotificationRepository(db)
            fundRepo = FundraiserRepository(db)
            adminRepo = AdminRepository(db)
        } else {
            chatRepo = null; authRepo = null; itemRepo = null; grainsRepo = null
            socialRepo = null; notifRepo = null; fundRepo = null; adminRepo = null
        }
    }

    // ------------------------------------------------------------------ Messages et occupation
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()
    fun setThemeMode(mode: ThemeMode) { _themeMode.value = mode }

    private fun say(text: String) { _messages.tryEmit(text) }

    private fun friendly(e: Throwable): String = when (e) {
        is FirebaseAuthInvalidCredentialsException -> "E-mail ou mot de passe incorrect."
        is FirebaseAuthInvalidUserException -> "Aucun compte avec cet e-mail."
        is FirebaseAuthUserCollisionException -> "Un compte existe déjà avec cet e-mail."
        is FirebaseAuthWeakPasswordException -> "Mot de passe trop faible (6 caractères minimum)."
        is FirebaseNetworkException -> "Pas de connexion internet. Réessayez."
        is FirebaseFirestoreException -> when (e.code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                "Action refusée. Votre statut ou l'état de l'échange ne le permet pas."
            FirebaseFirestoreException.Code.UNAVAILABLE -> "Pas de connexion internet. Réessayez."
            else -> "Une erreur est survenue. Réessayez."
        }
        is IllegalStateException -> e.message ?: "Action impossible."
        else -> e.message?.takeIf { it.length < 120 } ?: "Une erreur est survenue. Réessayez."
    }

    private fun action(success: String? = null, after: (() -> Unit)? = null, block: suspend () -> Result<*>) {
        viewModelScope.launch {
            _busy.value = true
            val result = try { block() } catch (e: Exception) { Result.failure<Unit>(e) }
            _busy.value = false
            result.onSuccess {
                success?.let { say(it) }
                after?.invoke()
            }.onFailure { say(friendly(it)) }
        }
    }

    // ------------------------------------------------------------------ Session
    private val authUser: StateFlow<FirebaseUser?> = (authRepo?.authState() ?: flowOf(null))
        .stateIn(viewModelScope, SharingStarted.Eagerly, auth?.currentUser)

    private val uidFlow: Flow<String?> = authUser.map { it?.uid }.distinctUntilChanged()

    val session: StateFlow<Session> = authUser.flatMapLatest { user ->
        if (user == null || authRepo == null) flowOf<Session>(Session.SignedOut)
        else authRepo.profileFlow(user.uid)
            .map<UserProfile?, Session> { p -> if (p == null) Session.NeedsProfile(user) else Session.Active(p) }
            .onStart { emit(Session.Loading) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, if (auth?.currentUser == null) Session.SignedOut else Session.Loading)

    val profile: StateFlow<UserProfile?> = session.map { (it as? Session.Active)?.profile }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val myId: String? get() = profile.value?.id

    // ------------------------------------------------------------------ Navigation (pile)
    private val _stack = MutableStateFlow<List<Route>>(listOf(Route.Home))
    val stack: StateFlow<List<Route>> = _stack.asStateFlow()
    val currentRoute: Route get() = _stack.value.last()

    fun resetNavigation() {
        val p = profile.value
        _stack.value = listOf(if (p != null) Tabs.startRoute(p) else Route.Home)
    }

    fun openTab(route: Route) { _stack.value = listOf(route) }

    fun push(route: Route) {
        val p = profile.value
        if (p != null) {
            if (p.isMediator && route.forbiddenForMediator()) return
            if (!p.isMediator && route.requiresMediator()) return
        }
        if (_stack.value.last() == route) return
        _stack.value = _stack.value + route
    }

    /** Retourne false si l'on est déjà à la racine. */
    fun back(): Boolean {
        val s = _stack.value
        if (s.size <= 1) return false
        _stack.value = s.dropLast(1)
        return true
    }

    // ------------------------------------------------------------------ Données temps réel
    val items: StateFlow<List<Item>> = uidFlow.flatMapLatest { uid ->
        if (uid == null || itemRepo == null) flowOf(emptyList()) else itemRepo.itemsFlow()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val myRequests: StateFlow<List<ItemRequest>> = uidFlow.flatMapLatest { uid ->
        if (uid == null || itemRepo == null) flowOf(emptyList()) else itemRepo.myRequestsFlow(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val incomingRequests: StateFlow<List<ItemRequest>> = uidFlow.flatMapLatest { uid ->
        if (uid == null || itemRepo == null) flowOf(emptyList()) else itemRepo.incomingRequestsFlow(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val threads: StateFlow<List<ChatThread>> = uidFlow.flatMapLatest { uid ->
        if (uid == null || chatRepo == null) flowOf(emptyList()) else chatRepo.threadsFlow(uid)
    }.map { list -> list.sortedByDescending { it.lastMessageAtMs } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notifications: StateFlow<List<AppNotification>> = uidFlow.flatMapLatest { uid ->
        if (uid == null || notifRepo == null) flowOf(emptyList()) else notifRepo.notificationsFlow(uid)
    }.map { list -> list.sortedByDescending { it.createdAtMs } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val ledger: StateFlow<List<LedgerEntry>> = uidFlow.flatMapLatest { uid ->
        if (uid == null || socialRepo == null) flowOf(emptyList()) else socialRepo.ledgerFlow(uid)
    }.map { list -> list.sortedByDescending { it.createdAtMs } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val fundraisers: StateFlow<List<Fundraiser>> = uidFlow.flatMapLatest { uid ->
        if (uid == null || fundRepo == null) flowOf(emptyList()) else fundRepo.fundraisersFlow()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val collectionPoints: StateFlow<List<CollectionPoint>> = uidFlow.flatMapLatest { uid ->
        if (uid == null || fundRepo == null) flowOf(emptyList()) else fundRepo.pointsFlow()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val myContributions: StateFlow<List<Contribution>> = uidFlow.flatMapLatest { uid ->
        if (uid == null || fundRepo == null) flowOf(emptyList()) else fundRepo.myContributionsFlow(uid)
    }.map { list -> list.sortedByDescending { it.createdAtMs } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Contributions reçues par mon association (promesses à confirmer + confirmées). */
    val contributionsReceived: StateFlow<List<Contribution>> = profile.map { it?.takeIf { p -> p.isAssociation }?.id }
        .distinctUntilChanged().flatMapLatest { id ->
            if (id == null || fundRepo == null) flowOf(emptyList()) else fundRepo.contributionsReceivedFlow(id)
        }.map { list -> list.sortedByDescending { it.createdAtMs } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val disputes: StateFlow<List<Dispute>> = profile.map { p -> p?.let { Pair(it.id, it.isMediator) } }
        .distinctUntilChanged().flatMapLatest { pair ->
            if (pair == null || adminRepo == null) flowOf(emptyList()) else adminRepo.disputesFlow(pair.first, pair.second)
        }.map { list -> list.sortedByDescending { it.createdAtMs } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingAssociations: StateFlow<List<UserProfile>> = profile.map { it?.isMediator == true }
        .distinctUntilChanged().flatMapLatest { isMediator ->
            if (!isMediator || adminRepo == null) flowOf(emptyList()) else adminRepo.pendingAssociationsFlow()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun messagesFlow(threadId: String): Flow<List<ChatMessage>> = chatRepo?.messagesFlow(threadId) ?: emptyFlow()
    fun threadFlow(threadId: String): Flow<ChatThread?> = chatRepo?.threadFlow(threadId) ?: flowOf(null)
    fun publicProfileFlow(uid: String): Flow<UserProfile?> = socialRepo?.publicProfileFlow(uid) ?: flowOf(null)
    fun reviewsFlow(uid: String): Flow<List<Review>> =
        (socialRepo?.reviewsFlow(uid) ?: flowOf(emptyList())).map { l -> l.sortedByDescending { it.createdAtMs } }
    fun myReviewsWrittenFlow(): Flow<List<Review>> = myId?.let { socialRepo?.myReviewsWrittenFlow(it) } ?: flowOf(emptyList())

    // ------------------------------------------------------------------ Position et filtres
    private val _filters = MutableStateFlow(HomeFilters())
    val filters: StateFlow<HomeFilters> = _filters.asStateFlow()
    fun setFilters(f: HomeFilters) { _filters.value = f }

    private val _viewMode = MutableStateFlow(HomeViewMode.LIST)
    val viewMode: StateFlow<HomeViewMode> = _viewMode.asStateFlow()
    fun setViewMode(m: HomeViewMode) { _viewMode.value = m }

    /** Position de référence pour les distances : profil, sinon centre de la ville. */
    private val reference: StateFlow<Pair<Double, Double>> = profile.map { p ->
        when {
            p == null -> Pair(Cities.all.first().lat, Cities.all.first().lng)
            p.lat != 0.0 || p.lng != 0.0 -> Pair(p.lat, p.lng)
            else -> Cities.find(p.city).let { Pair(it.lat, it.lng) }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, Pair(Cities.all.first().lat, Cities.all.first().lng))

    /** Annonces disponibles, avec distance, filtrées et triées (les plus proches d'abord). */
    val homeItems: StateFlow<List<Item>> = combine(items, _filters, reference, profile) { all, f, ref, p ->
        all.asSequence()
            .filter { it.statusEnum == ItemStatus.AVAILABLE }
            .filter { p == null || !p.isSuspended }
            .map { it.copy(distanceMeters = LocationUtils.distanceMeters(ref.first, ref.second, it.approximateLat, it.approximateLng)) }
            .filter { f.category == Category.TOUT || it.category == f.category.name }
            .filter { f.exchange == null || it.exchangeType == f.exchange.name }
            .filter { !f.onlyEmergency || it.isEmergencyResponse }
            .filter { f.radiusKm == null || it.distanceMeters <= f.radiusKm * 1000 }
            .filter {
                f.query.isBlank() || it.title.contains(f.query, true) || it.description.contains(f.query, true) ||
                        it.neighborhood.contains(f.query, true) || it.landmark.contains(f.query, true)
            }
            .sortedWith(compareByDescending<Item> { it.isEmergencyResponse }.thenBy { it.distanceMeters })
            .toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun distanceTo(item: Item): Int {
        val ref = reference.value
        return LocationUtils.distanceMeters(ref.first, ref.second, item.approximateLat, item.approximateLng)
    }

    // ------------------------------------------------------------------ Authentification
    fun clearAuthError() { _authError.value = null }

    /** Mode « première installation » : le prochain profil créé sera celui de l'administrateur. */
    private val _adminSetupMode = MutableStateFlow(false)
    val adminSetupMode: StateFlow<Boolean> = _adminSetupMode.asStateFlow()
    fun setAdminSetupMode(on: Boolean) { _adminSetupMode.value = on }

    fun signIn(email: String, password: String) {
        val repo = authRepo ?: return
        viewModelScope.launch {
            _busy.value = true; _authError.value = null
            repo.signIn(email, password).onFailure { _authError.value = friendly(it) }
            _busy.value = false
        }
    }

    fun register(email: String, password: String) {
        val repo = authRepo ?: return
        viewModelScope.launch {
            _busy.value = true; _authError.value = null
            repo.register(email, password).onFailure { _authError.value = friendly(it) }
            _busy.value = false
        }
    }

    fun resetPassword(email: String) {
        val repo = authRepo ?: return
        if (email.isBlank()) { say("Saisissez d'abord votre adresse e-mail."); return }
        action("Un e-mail de réinitialisation vous a été envoyé.") { repo.sendPasswordReset(email) }
    }

    fun reportGoogleError(message: String) { _authError.value = message }

    fun completeProfile(form: OnboardingForm) {
        val repo = authRepo ?: return
        val user = authUser.value ?: return
        action { repo.createProfile(user, form) }
    }

    fun completeAdminSetup(form: OnboardingForm, setupCode: String) {
        val repo = authRepo ?: return
        val user = authUser.value ?: return
        action(after = { }) {
            repo.createAdminProfile(user, form, setupCode).also { r ->
                if (r.isFailure) throw IllegalStateException("Code de configuration invalide ou administrateur déjà créé.")
            }
        }
    }

    suspend fun adminExists(): Boolean = authRepo?.adminExists() ?: true

    fun signOut() {
        authRepo?.signOut()
        _stack.value = listOf(Route.Home)
    }

    // ------------------------------------------------------------------ Publication
    private val _suggestion = MutableStateFlow<AiSuggestion?>(null)
    val suggestion: StateFlow<AiSuggestion?> = _suggestion.asStateFlow()
    private val _suggesting = MutableStateFlow(false)
    val suggesting: StateFlow<Boolean> = _suggesting.asStateFlow()

    fun suggest(bitmap: Bitmap?, hint: String) {
        viewModelScope.launch {
            _suggesting.value = true
            val s = aiRepo.suggest(bitmap, hint)
            _suggesting.value = false
            if (s == null) say("L'assistant IA est indisponible. Remplissez les champs à la main.")
            _suggestion.value = s
        }
    }

    fun clearSuggestion() { _suggestion.value = null }

    fun publish(
        title: String, description: String, category: Category,
        condition: com.example.data.model.ItemCondition, exchangeType: ExchangeType,
        wantedInReturn: String, bitmap: Bitmap?, emergency: Boolean, orphanage: String,
        onDone: () -> Unit
    ) {
        val p = profile.value ?: return
        val repo = itemRepo ?: return
        viewModelScope.launch {
            _busy.value = true
            val b64 = if (bitmap != null) withContext(Dispatchers.Default) { ImageUtils.toBase64(bitmap) } else ""
            val result = repo.publish(
                p,
                PublishForm(title, description, category, condition, exchangeType, wantedInReturn, b64, emergency, orphanage)
            )
            _busy.value = false
            result.onSuccess { item ->
                _suggestion.value = null
                say(
                    if (item.statusEnum == ItemStatus.AVAILABLE) "Votre annonce est en ligne."
                    else "Annonce envoyée : un médiateur la valide sous peu."
                )
                onDone()
            }.onFailure { say(friendly(it)) }
        }
    }

    fun deleteItem(item: Item, onDone: () -> Unit = {}) {
        val repo = itemRepo ?: return
        action("Annonce supprimée.", after = onDone) { repo.delete(item) }
    }

    // ------------------------------------------------------------------ Échanges
    fun requestItem(item: Item, message: String, topUp: Int, onDone: (String) -> Unit) {
        val p = profile.value ?: return
        val repo = itemRepo ?: return
        val previous = myRequests.value.firstOrNull { it.itemId == item.id }
        action(
            success = if (item.isTroc) "Proposition envoyée." else "Demande envoyée.",
            after = {
                viewModelScope.launch {
                    notifRepo?.send(
                        item.donorId, p.id, NotificationType.EXCHANGE,
                        if (item.isTroc) "Nouvelle proposition de troc" else "Nouvelle demande",
                        "${p.displayName} est intéressé(e) par « ${item.title} »", item.id
                    )
                }
                onDone("${item.id}_${p.id}")
            }
        ) { repo.requestItem(item, p, message, topUp, previous) }
    }

    fun cancelRequest(request: ItemRequest) {
        val repo = itemRepo ?: return
        action("Demande annulée.") { repo.cancelRequest(request) }
    }

    fun declineRequest(request: ItemRequest) {
        val repo = itemRepo ?: return
        val p = profile.value ?: return
        action("Demande refusée.", after = {
            viewModelScope.launch {
                notifRepo?.send(request.requesterId, p.id, NotificationType.EXCHANGE, "Demande refusée",
                    "Votre demande pour « ${request.itemTitle} » n'a pas été retenue.", request.itemId)
            }
        }) { repo.declineRequest(request) }
    }

    fun acceptRequest(item: Item, request: ItemRequest) {
        val repo = itemRepo ?: return
        val p = profile.value ?: return
        action("Demande acceptée. Convenez d'un lieu public pour la remise.", after = {
            viewModelScope.launch {
                notifRepo?.send(request.requesterId, p.id, NotificationType.EXCHANGE, "Demande acceptée 🎉",
                    "Vous pouvez récupérer « ${item.title} ». Discutez du lieu de rencontre.", item.id)
            }
        }) { repo.acceptRequest(item, request, incomingRequests.value) }
    }

    fun confirmHandover(item: Item) {
        val repo = itemRepo ?: return
        val p = profile.value ?: return
        action("Remise confirmée.", after = {
            val other = if (p.id == item.donorId) item.reservedByUserId else item.donorId
            viewModelScope.launch {
                notifRepo?.send(other, p.id, NotificationType.EXCHANGE, "Remise confirmée",
                    "${p.displayName} a confirmé la remise de « ${item.title} ».", item.id)
            }
        }) { repo.confirmHandover(item.id, p.id) }
    }

    fun cancelReservation(item: Item) {
        val repo = itemRepo ?: return
        val p = profile.value ?: return
        action("Réservation annulée.", after = {
            val other = if (p.id == item.donorId) item.reservedByUserId else item.donorId
            viewModelScope.launch {
                notifRepo?.send(other, p.id, NotificationType.EXCHANGE, "Réservation annulée",
                    "${p.displayName} a annulé l'échange de « ${item.title} ». L'objet est de nouveau disponible.", item.id)
            }
        }) { repo.cancelReservation(item, p.id, p.displayName) }
    }

    fun sendMessage(thread: ChatThread, text: String) {
        val p = profile.value ?: return
        val repo = chatRepo ?: return
        if (text.isBlank()) return
        viewModelScope.launch {
            val seed = ThreadSeed(thread.itemId, thread.itemTitle, thread.ownerId, thread.ownerName, thread.requesterId, thread.requesterName)
            repo.send(seed, p.id, p.displayName, text).onFailure { say(friendly(it)) }
        }
    }

    // ------------------------------------------------------------------ Avis et signalements
    fun submitReview(item: Item, targetUserId: String, rating: Int, badge: String, comment: String) {
        val p = profile.value ?: return
        val social = socialRepo ?: return
        val grains = grainsRepo ?: return
        action("Merci pour votre avis !", after = {
            viewModelScope.launch {
                grains.claim(p.id, GrainsRepository.Claim(LedgerReason.REVIEW, GrainRules.REVIEW, item.id))
                notifRepo?.send(targetUserId, p.id, NotificationType.EXCHANGE, "Nouvel avis reçu",
                    "${p.displayName} vous a laissé $rating★ pour « ${item.title} ».", item.id)
            }
        }) { social.addReview(item, p, targetUserId, rating, badge, comment) }
    }

    fun reportItem(item: Item, reason: DisputeReason, details: String) {
        val p = profile.value ?: return
        val repo = adminRepo ?: return
        action("Signalement transmis à la médiation. Merci pour votre vigilance.") {
            repo.reportItem(p, item, reason, details)
        }
    }

    // ------------------------------------------------------------------ Graines : règlement des échanges terminés
    private val failedClaims = mutableMapOf<String, Long>()

    private suspend fun settleExchanges() {
        val p = profile.value ?: return
        val grains = grainsRepo ?: return
        val done = items.value.filter {
            it.statusEnum == ItemStatus.COMPLETED && (it.donorId == p.id || it.reservedByUserId == p.id)
        }
        if (done.isEmpty()) return
        val ledgerIds = ledger.value.map { it.id }.toSet()
        for (item in done) {
            val request = if (item.donorId == p.id)
                incomingRequests.value.firstOrNull { it.itemId == item.id && it.requesterId == item.reservedByUserId }
            else myRequests.value.firstOrNull { it.itemId == item.id }
            for (claim in grains.exchangeClaims(item, request, p.id)) {
                val key = "${claim.refId}_${claim.reason.name}"
                if (key in ledgerIds) continue
                val last = failedClaims[key]
                if (last != null && System.currentTimeMillis() - last < 30_000) continue
                val result = grains.claim(p.id, claim)
                if (result.isFailure) {
                    failedClaims[key] = System.currentTimeMillis()
                } else if (result.getOrNull() == true) {
                    failedClaims.remove(key)
                    val sign = if (claim.amount > 0) "+" else ""
                    say("$sign${claim.amount} 🌾 Graines : ${claim.reason.label.lowercase()}")
                }
            }
        }
    }

    init {
        viewModelScope.launch {
            combine(items, myRequests, incomingRequests, ledger, profile) { _, _, _, _, _ -> Unit }
                .conflate()
                .collect { settleExchanges() }
        }
    }

    // ------------------------------------------------------------------ Profil
    fun updateLocation(city: String, neighborhood: String, landmark: String, lat: Double, lng: Double, radiusKm: Float) {
        val p = profile.value ?: return
        val repo = authRepo ?: return
        action("Localisation enregistrée.", after = { _filters.value = _filters.value.copy(radiusKm = radiusKm) }) {
            repo.updateLocation(p.id, city, neighborhood, landmark, lat, lng, radiusKm)
        }
    }

    fun resubmitAssociation(name: String, type: String, reg: String, phone: String) {
        val p = profile.value ?: return
        val repo = authRepo ?: return
        action("Demande envoyée de nouveau au médiateur.") { repo.resubmitAssociation(p.id, name, type, reg, phone) }
    }

    fun markNotificationsRead() {
        val list = notifications.value
        viewModelScope.launch { notifRepo?.markAllRead(list) }
    }

    // ------------------------------------------------------------------ Cagnottes et relais
    fun createFundraiser(form: FundraiserForm, onDone: () -> Unit) {
        val p = profile.value ?: return
        val repo = fundRepo ?: return
        action("Cagnotte envoyée : un médiateur la valide avant publication.", after = onDone) { repo.create(p, form) }
    }

    fun giveGrains(f: Fundraiser, grains: Int, anonymous: Boolean, onDone: () -> Unit) {
        val p = profile.value ?: return
        val repo = fundRepo ?: return
        action("Merci ! Vos Graines ont été versées à la cagnotte.", after = onDone) { repo.giveGrains(f, p, grains, anonymous) }
    }

    fun pledgeFcfa(f: Fundraiser, amount: Long, anonymous: Boolean, onDone: () -> Unit) {
        val p = profile.value ?: return
        val repo = fundRepo ?: return
        action("Promesse enregistrée. Suivez les instructions de l'association pour remettre votre don.", after = onDone) {
            repo.pledgeFcfa(f, p, amount, anonymous)
        }
    }

    fun confirmPledge(c: Contribution, f: Fundraiser, received: Boolean) {
        val repo = fundRepo ?: return
        val p = profile.value ?: return
        action(if (received) "Don confirmé, merci à votre donateur." else "Promesse marquée comme non reçue.", after = {
            viewModelScope.launch {
                notifRepo?.send(c.contributorId, p.id, NotificationType.FUNDRAISER,
                    if (received) "Don reçu 🙏" else "Don non reçu",
                    if (received) "${f.associationName} confirme avoir reçu votre don de ${c.amountFcfa} FCFA."
                    else "${f.associationName} n'a pas pu confirmer votre don de ${c.amountFcfa} FCFA.", f.id)
            }
        }) { repo.confirmPledge(c, f, received) }
    }

    fun closeFundraiser(f: Fundraiser) {
        val repo = fundRepo ?: return
        action("Cagnotte clôturée.") { repo.close(f) }
    }

    fun createPoint(form: CollectionPointForm, onDone: () -> Unit) {
        val p = profile.value ?: return
        val repo = fundRepo ?: return
        action("Point relais ajouté.", after = onDone) { repo.createPoint(p, form) }
    }

    fun deletePoint(point: CollectionPoint) {
        val repo = fundRepo ?: return
        action("Point relais supprimé.") { repo.deletePoint(point) }
    }

    // ------------------------------------------------------------------ Médiation
    fun moderateItem(item: Item, approve: Boolean, note: String) {
        val repo = itemRepo ?: return
        val p = profile.value ?: return
        action(if (approve) "Annonce approuvée." else "Annonce refusée.", after = {
            viewModelScope.launch {
                notifRepo?.send(item.donorId, p.id, NotificationType.MODERATION,
                    if (approve) "Annonce validée ✅" else "Annonce refusée",
                    if (approve) "« ${item.title} » est maintenant visible par vos voisins."
                    else "« ${item.title} » a été refusée. ${note.trim()}", item.id)
            }
        }) { repo.moderate(item, approve, note) }
    }

    fun setItemSuspended(item: Item, suspended: Boolean, note: String) {
        val repo = itemRepo ?: return
        action(if (suspended) "Annonce suspendue." else "Annonce remise en ligne.") { repo.setSuspended(item, suspended, note) }
    }

    fun takeDispute(d: Dispute) {
        val repo = adminRepo ?: return
        val p = profile.value ?: return
        action("Litige pris en charge.") { repo.takeDispute(d, p.id) }
    }

    fun resolveDispute(d: Dispute, dismissed: Boolean, note: String, act: DisputeAction) {
        val repo = adminRepo ?: return
        val p = profile.value ?: return
        action("Litige clos.", after = {
            viewModelScope.launch {
                notifRepo?.send(d.reporterId, p.id, NotificationType.DISPUTE, "Votre signalement a été traité",
                    if (dismissed) "Après examen, aucune sanction n'a été retenue." else "Une décision a été prise : ${act.label.lowercase()}.", d.itemId)
                if (!dismissed && act != DisputeAction.NONE) {
                    notifRepo?.send(d.reportedUserId, p.id, NotificationType.DISPUTE, "Décision de la médiation",
                        "À la suite d'un signalement concernant « ${d.itemTitle} » : ${act.label.lowercase()}. ${note.trim()}", d.itemId)
                }
            }
        }) { repo.resolveDispute(d, p.id, dismissed, note, act) }
    }

    fun certifyAssociation(user: UserProfile, approve: Boolean, note: String) {
        val repo = adminRepo ?: return
        val p = profile.value ?: return
        action(if (approve) "Association certifiée." else "Certification refusée.", after = {
            viewModelScope.launch {
                notifRepo?.send(user.id, p.id, NotificationType.ASSOCIATION,
                    if (approve) "Association certifiée 🎉" else "Certification refusée",
                    if (approve) "Vous pouvez maintenant publier des dons, créer des cagnottes et des points relais."
                    else "Motif : ${note.trim().ifBlank { "dossier incomplet" }}. Corrigez vos informations et renvoyez la demande.")
            }
        }) { repo.certifyAssociation(user, approve, note) }
    }

    fun reviewFundraiser(f: Fundraiser, approve: Boolean) {
        val repo = fundRepo ?: return
        val p = profile.value ?: return
        action(if (approve) "Cagnotte publiée." else "Cagnotte refusée.", after = {
            viewModelScope.launch {
                notifRepo?.send(f.associationId, p.id, NotificationType.FUNDRAISER,
                    if (approve) "Cagnotte publiée ✅" else "Cagnotte refusée",
                    if (approve) "« ${f.title} » est visible par tous les membres."
                    else "« ${f.title} » n'a pas été validée. Contactez la médiation pour en savoir plus.", f.id)
            }
        }) { repo.review(f, approve) }
    }
}

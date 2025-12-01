package com.example.pasteleriamilsabores.navigation

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.pasteleriamilsabores.data.local.AppDatabase
import com.example.pasteleriamilsabores.data.model.ApiProduct
import com.example.pasteleriamilsabores.data.repo.CartRepository
import com.example.pasteleriamilsabores.data.repo.ProductRepository
import com.example.pasteleriamilsabores.ui.screen.ApiProductDetailScreen
import com.example.pasteleriamilsabores.ui.screen.CartScreen
import com.example.pasteleriamilsabores.ui.screen.HomeScreen
import com.example.pasteleriamilsabores.ui.screen.LoginScreen
import com.example.pasteleriamilsabores.ui.screen.ProductDetailScreen
import com.example.pasteleriamilsabores.ui.screen.ProductListScreen
import com.example.pasteleriamilsabores.ui.screen.ProductManagerHomeScreen
import com.example.pasteleriamilsabores.ui.screen.RegisterScreen
import com.example.pasteleriamilsabores.viewmodel.CartViewModel
import com.example.pasteleriamilsabores.viewmodel.LoginViewModel
import com.example.pasteleriamilsabores.viewmodel.ProductManagerViewModel
import com.example.pasteleriamilsabores.viewmodel.ProductViewModel
import com.example.pasteleriamilsabores.viewmodel.RegisterViewModel

sealed class Screen(val route: String) {
    object Register : Screen("register")
    object Login : Screen("login")
    object Home: Screen("home")
    object Detail: Screen("detail/{productId}") {
        fun createRoute(id: Int) = "detail/$id"
    }
    object Cart: Screen("cart")

    // NUEVAS PANTALLAS PARA GESTOR DE PRODUCTOS
    object ProductManagerHome : Screen("product_manager_home")
    object ProductList : Screen("product_list/{source}") {
        fun createRoute(source: String) = "product_list/$source"
    }
    object ApiProductDetail : Screen("api_product_detail")
}

@Composable
fun NavGraph(
    appContext: Context,
    navController: NavHostController,
    paddingValues: PaddingValues
) {
    val db = AppDatabase.getInstance(appContext)
    val productRepository = ProductRepository(appContext)
    val cartRepository = CartRepository(db)

    val productViewModel: ProductViewModel = viewModel(factory = ProductViewModel.factory(productRepository))
    val cartViewModel: CartViewModel = viewModel(factory = CartViewModel.factory(cartRepository))
    val registerViewModel: RegisterViewModel = viewModel(factory = RegisterViewModel.factory())
    val loginViewModel: LoginViewModel = viewModel(factory = LoginViewModel.factory())

    NavHost(
        navController = navController,
        startDestination = Screen.Login.route,
        modifier = Modifier.padding(paddingValues)
    ) {
        // ========== PANTALLAS EXISTENTES DE PASTELERÍA ==========
        composable(Screen.Login.route) {
            LoginScreen(
                paddingValues = PaddingValues(16.dp),
                viewModel = loginViewModel,
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onRegisterClick = {
                    navController.navigate(Screen.Register.route)
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                paddingValues = PaddingValues(16.dp),
                viewModel = registerViewModel,
                onRegisterSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                },
                onBackToLogin = { navController.popBackStack() }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = productViewModel,
                cartViewModel = cartViewModel,
                onProductClick = { id -> navController.navigate(Screen.Detail.createRoute(id)) },
                onCartClick = { navController.navigate(Screen.Cart.route) },
                // NUEVO: Agregado para navegar al Gestor de Productos
                onProductManagerClick = {
                    navController.navigate(Screen.ProductManagerHome.route)
                }
            )
        }

        composable(
            route = Screen.Detail.route,
            arguments = listOf(navArgument("productId") { type = NavType.IntType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getInt("productId") ?: 0
            ProductDetailScreen(
                productId = id,
                viewModel = productViewModel,
                cartViewModel = cartViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Cart.route) {
            CartScreen(
                viewModel = cartViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        // ========== NUEVAS PANTALLAS PARA GESTOR DE PRODUCTOS ==========
        composable(Screen.ProductManagerHome.route) {
            val productManagerViewModel: ProductManagerViewModel = viewModel(
                factory = ProductManagerViewModel.factory(productRepository)
            )

            val hasLocalData by productManagerViewModel.hasLocalData.collectAsState()

            ProductManagerHomeScreen(
                onLoadFromApi = {
                    navController.navigate(Screen.ProductList.createRoute("api"))
                },
                onLoadFromDatabase = {
                    navController.navigate(Screen.ProductList.createRoute("database"))
                },
                onBack = { navController.popBackStack() },
                isOnline = productManagerViewModel.isOnline(),
                hasLocalData = hasLocalData
            )
        }

        composable(
            route = Screen.ProductList.route,
            arguments = listOf(navArgument("source") { type = NavType.StringType })
        ) { backStackEntry ->
            val source = backStackEntry.arguments?.getString("source") ?: "api"
            val productManagerViewModel: ProductManagerViewModel = viewModel(
                factory = ProductManagerViewModel.factory(productRepository)
            )

            // Cargar productos según la fuente
            LaunchedEffect(source) {
                when (source) {
                    "api" -> productManagerViewModel.loadFromApi()
                    "database" -> productManagerViewModel.loadFromDatabase()
                }
            }

            ProductListScreen(
                viewModel = productManagerViewModel,
                onProductClick = { product ->
                    // Guardar producto para pasarlo a la pantalla de detalle
                    navController.currentBackStackEntry?.savedStateHandle?.set(
                        "selected_api_product", product
                    )
                    navController.navigate(Screen.ApiProductDetail.route)
                },
                onBack = { navController.popBackStack() },
                source = source
            )
        }

        composable(Screen.ApiProductDetail.route) {
            // Recuperar producto de la pantalla anterior
            val product = navController.previousBackStackEntry
                ?.savedStateHandle
                ?.get<ApiProduct>("selected_api_product")

            if (product != null) {
                ApiProductDetailScreen(
                    product = product,
                    onBack = { navController.popBackStack() }
                )
            } else {
                // Manejar error - producto no encontrado
                ErrorProductNotFound(
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

// Composable auxiliar para manejar errores
@Composable
fun ErrorProductNotFound(onBack: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Error: Producto no encontrado",
                style = androidx.compose.material3.MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onBack) {
                Text("Volver al listado")
            }
        }
    }
}
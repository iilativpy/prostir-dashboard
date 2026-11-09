package com.prostir.dashboard.ui.screens

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prostir.dashboard.R
import com.prostir.dashboard.data.ApiError
import com.prostir.dashboard.ui.LoginViewModel
import com.prostir.dashboard.ui.components.Glyph
import com.prostir.dashboard.ui.components.ProCard
import com.prostir.dashboard.ui.components.ProIcon
import com.prostir.dashboard.ui.theme.Onest
import com.prostir.dashboard.ui.theme.Pro

@Composable
fun LoginScreen(vm: LoginViewModel, sessionExpired: Boolean, onLoggedIn: () -> Unit) {
    val c = Pro.c
    val state by vm.state.collectAsStateWithLifecycle()
    val focus = LocalFocusManager.current
    val submit = { vm.submit(Build.MODEL ?: "Android") { onLoggedIn() } }

    BoxWithConstraints(Modifier.fillMaxSize().systemBarsPadding().imePadding()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.logo_wordmark),
                contentDescription = "Простір",
                tint = c.ink,
                modifier = Modifier.height(28.dp),
            )
            Text("Чотири будинки. Одна панель.", color = c.muted, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp, bottom = 28.dp))

            if (sessionExpired) {
                ProCard(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ProIcon(Glyph.Lock, c.ink, size = 22.dp)
                        Column {
                            Text("Потрібно увійти знову", fontWeight = FontWeight.SemiBold)
                            Text("Сеанс завершено. Увійдіть, щоб продовжити.", color = c.muted, fontSize = 12.5.sp)
                        }
                    }
                }
            }

            val failed = state.error is ApiError.InvalidCredentials
            Field(
                label = "Логін", value = state.login, onValueChange = vm::onLogin, isError = failed,
                imeAction = ImeAction.Next,
                onImeAction = { focus.moveFocus(FocusDirection.Down) },
            )
            Spacer(Modifier.height(14.dp))
            Field(
                label = "Пароль", value = state.password, onValueChange = vm::onPassword, isError = failed,
                password = true, showPassword = state.showPassword, onToggle = vm::toggleShowPassword,
                imeAction = ImeAction.Done,
                onImeAction = { focus.clearFocus(); submit() },
            )

            val message = errorMessage(state.error)
            if (message != null) {
                Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ProIcon(Glyph.Alert, c.bad, size = 16.dp, modifier = Modifier.padding(top = 1.dp))
                    Text(message, color = c.bad, fontSize = 12.5.sp)
                }
            }

            Spacer(Modifier.height(20.dp))
            val label = when {
                state.lockSeconds > 0 -> "Спробувати за ${state.lockSeconds / 60}:${"%02d".format(state.lockSeconds % 60)}"
                else -> "Увійти"
            }
            val locked = state.lockSeconds > 0
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .alpha(if (state.canSubmit || state.loading) 1f else 0.5f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (locked) c.surface2 else c.accent)
                    .clickable(enabled = state.canSubmit, role = Role.Button) { focus.clearFocus(); submit() },
                contentAlignment = Alignment.Center,
            ) {
                if (state.loading) {
                    CircularProgressIndicator(Modifier.size(22.dp), color = c.onAccent, strokeWidth = 2.5.dp)
                } else {
                    Text(label, color = if (locked) c.ink else c.onAccent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

private fun errorMessage(error: ApiError?): String? = when (error) {
    null -> null
    ApiError.InvalidCredentials -> "Невірний логін або пароль. Перевірте дані й спробуйте ще раз."
    is ApiError.TooManyAttempts -> "Забагато спроб. Спробуйте ще раз за хвилину."
    is ApiError.Validation -> "Введіть логін і пароль."
    ApiError.Network -> "Немає з’єднання з сервером. Перевірте мережу."
    ApiError.Server -> "Сервер тимчасово недоступний. Спробуйте пізніше."
    ApiError.SessionExpired -> null
    is ApiError.Unknown -> error.message ?: "Не вдалося увійти."
}

@Composable
private fun Field(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    imeAction: ImeAction,
    onImeAction: () -> Unit,
    password: Boolean = false,
    showPassword: Boolean = false,
    onToggle: (() -> Unit)? = null,
) {
    val c = Pro.c
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = c.muted, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
        Row(
            Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(c.surface)
                .border(BorderStroke(1.5.dp, if (isError) c.bad else c.line), RoundedCornerShape(16.dp))
                .padding(start = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(color = c.ink, fontSize = 15.sp, fontFamily = Onest),
                cursorBrush = SolidColor(c.accent),
                visualTransformation = if (password && !showPassword) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (password) KeyboardType.Password else KeyboardType.Text,
                    imeAction = imeAction,
                ),
                keyboardActions = KeyboardActions(onNext = { onImeAction() }, onDone = { onImeAction() }),
                modifier = Modifier.weight(1f),
            )
            if (onToggle != null) {
                Box(
                    Modifier.heightIn(min = 48.dp).clickable(role = Role.Button, onClick = onToggle).padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(if (showPassword) "Сховати" else "Показати", color = c.accentInk, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

package com.example.kosmos.shared.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.kosmos.shared.ui.designsystem.IconSet
import com.example.kosmos.shared.ui.designsystem.Tokens
import com.example.kosmos.shared.ui.designsystem.TypographyTokens
import com.example.kosmos.shared.ui.designsystem.ColorTokens

/**
 * Unified dialog styling defaults for Kosmos dark theme.
 * All dialogs/popups should use these values for visual consistency.
 */
object KosmosDialogDefaults {
    val containerColor get() = ColorTokens.ReactTheme.card
    val shape: Shape = RoundedCornerShape(Tokens.CornerRadius.lg) // 16dp
    val elevation = Tokens.Elevation.level3 // 6dp
    val titleStyle get() = TypographyTokens.Custom.dialogTitle
    val titleColor get() = ColorTokens.ReactTheme.foreground
    val subtitleColor get() = ColorTokens.ReactTheme.mutedForeground
    val dividerColor get() = ColorTokens.ReactTheme.border.copy(alpha = 0.3f)
    val primaryButtonColor get() = ColorTokens.ReactTheme.primary
    val dismissButtonColor get() = ColorTokens.ReactTheme.mutedForeground

    /** Standard OutlinedTextField colors for dialogs */
    @Composable
    fun textFieldColors() = OutlinedTextFieldDefaults.colors(
        focusedTextColor = ColorTokens.ReactTheme.foreground,
        unfocusedTextColor = ColorTokens.ReactTheme.foreground,
        focusedContainerColor = ColorTokens.ReactTheme.secondary,
        unfocusedContainerColor = ColorTokens.ReactTheme.secondary,
        focusedBorderColor = ColorTokens.ReactTheme.primary,
        unfocusedBorderColor = ColorTokens.ReactTheme.border,
        cursorColor = ColorTokens.ReactTheme.primary,
        focusedLabelColor = ColorTokens.ReactTheme.primary,
        unfocusedLabelColor = ColorTokens.ReactTheme.mutedForeground,
        errorBorderColor = ColorTokens.ReactTheme.destructive,
        focusedPlaceholderColor = ColorTokens.ReactTheme.mutedForeground,
        unfocusedPlaceholderColor = ColorTokens.ReactTheme.mutedForeground
    )
}

/**
 * Reusable dialog container with Kosmos dark theme styling.
 * Wraps Dialog + Surface with standard appearance.
 *
 * @param onDismissRequest Dismiss handler
 * @param modifier Modifier for the Surface
 * @param properties Dialog properties
 * @param content Dialog content
 */
@Composable
fun KosmosDialogSurface(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    properties: androidx.compose.ui.window.DialogProperties = androidx.compose.ui.window.DialogProperties(),
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties
    ) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = KosmosDialogDefaults.shape,
            color = KosmosDialogDefaults.containerColor,
            tonalElevation = KosmosDialogDefaults.elevation
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Tokens.Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.md),
                content = content
            )
        }
    }
}

/**
 * Dialog Components for Kosmos App
 *
 * Modal dialogs, bottom sheets, and confirmation dialogs.
 * Optimized for mobile with bottom sheets as primary pattern.
 *
 * Components:
 * - BottomSheetStandard: Standard modal bottom sheet
 * - BottomSheetWithHeader: Bottom sheet with dismissible header
 * - ConfirmationDialog: Standard confirmation dialog
 * - AlertDialog: Information/warning dialog
 * - LoadingDialog: Full-screen loading overlay
 * - FullScreenDialog: Full-screen modal
 */

/**
 * Standard Bottom Sheet
 *
 * Modal bottom sheet - preferred over dialogs on mobile
 * Easier to dismiss and more thumb-friendly
 *
 * @param onDismissRequest Dismiss handler
 * @param sheetState Bottom sheet state
 * @param modifier Modifier
 * @param dragHandle Whether to show drag handle
 * @param content Sheet content
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetStandard(
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(),
    modifier: Modifier = Modifier,
    dragHandle: @Composable (() -> Unit)? = { BottomSheetDefaults.DragHandle() },
    content: @Composable ColumnScope.() -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = modifier,
        dragHandle = dragHandle
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = Tokens.Spacing.md,
                    end = Tokens.Spacing.md,
                    bottom = Tokens.Spacing.xl
                ),
            content = content
        )
    }
}

/**
 * Bottom Sheet with Header
 *
 * Bottom sheet with title and close button
 * Common pattern for forms and detailed views
 *
 * @param title Sheet title
 * @param onDismissRequest Dismiss handler
 * @param sheetState Bottom sheet state
 * @param modifier Modifier
 * @param subtitle Optional subtitle
 * @param actions Optional header actions (save, etc.)
 * @param content Sheet content
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetWithHeader(
    title: String,
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(),
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = modifier,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Tokens.Spacing.md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = TypographyTokens.Custom.dialogTitle
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = TypographyTokens.Custom.caption,
                            color = ColorTokens.ReactTheme.mutedForeground
                        )
                    }
                }

                if (actions != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs),
                        content = actions
                    )
                }

                IconButtonStandard(
                    icon = IconSet.Navigation.close,
                    onClick = onDismissRequest,
                    contentDescription = "Close"
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = Tokens.Spacing.md)
            )

            // Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Tokens.Spacing.md)
                    .padding(bottom = Tokens.Spacing.xl),
                content = content
            )
        }
    }
}

/**
 * Confirmation Dialog
 *
 * Standard confirmation with title, message, and actions
 * Used for destructive actions and confirmations
 *
 * @param title Dialog title
 * @param message Dialog message
 * @param onConfirm Confirm button handler
 * @param onDismiss Dismiss/cancel handler
 * @param modifier Modifier
 * @param confirmText Confirm button text
 * @param dismissText Dismiss button text
 * @param icon Optional header icon
 * @param isDestructive Whether this is a destructive action (red button)
 */
@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    confirmText: String = "Confirm",
    dismissText: String = "Cancel",
    icon: ImageVector? = null,
    isDestructive: Boolean = false
) {
    KosmosDialogSurface(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        // Icon
        if (icon != null) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isDestructive)
                        ColorTokens.ReactTheme.destructive
                    else
                        ColorTokens.ReactTheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        // Title
        Text(
            text = title,
            style = KosmosDialogDefaults.titleStyle,
            color = KosmosDialogDefaults.titleColor
        )

        // Message
        Text(
            text = message,
            style = TypographyTokens.Custom.dialogContent,
            color = KosmosDialogDefaults.subtitleColor
        )

        // Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
        ) {
            SecondaryButton(
                text = dismissText,
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            )
            if (isDestructive) {
                DestructiveButton(
                    text = confirmText,
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f)
                )
            } else {
                PrimaryButton(
                    text = confirmText,
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Alert Dialog - Information/Warning
 *
 * Simple alert with OK button
 * Used for notifications, errors, warnings
 *
 * @param title Dialog title
 * @param message Dialog message
 * @param onDismiss Dismiss handler
 * @param modifier Modifier
 * @param buttonText Button text
 * @param icon Optional header icon
 * @param type Alert type (info, warning, error, success)
 */
@Composable
fun AlertDialogStandard(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    buttonText: String = "OK",
    icon: ImageVector? = null,
    type: AlertType = AlertType.INFO
) {
    val iconColor = when (type) {
        AlertType.INFO -> ColorTokens.ReactTheme.primary
        AlertType.WARNING -> ColorTokens.ReactTheme.accent
        AlertType.ERROR -> ColorTokens.ReactTheme.destructive
        AlertType.SUCCESS -> ColorTokens.ReactTheme.primary
    }

    val defaultIcon = when (type) {
        AlertType.INFO -> IconSet.Status.info
        AlertType.WARNING -> IconSet.Status.warning
        AlertType.ERROR -> IconSet.Status.error
        AlertType.SUCCESS -> IconSet.Status.success
    }

    KosmosDialogSurface(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon ?: defaultIcon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(32.dp)
            )
        }

        Text(
            text = title,
            style = KosmosDialogDefaults.titleStyle,
            color = KosmosDialogDefaults.titleColor
        )

        Text(
            text = message,
            style = TypographyTokens.Custom.dialogContent,
            color = KosmosDialogDefaults.subtitleColor
        )

        PrimaryButton(
            text = buttonText,
            onClick = onDismiss,
            fullWidth = true
        )
    }
}

/**
 * Alert Type Enum
 */
enum class AlertType {
    INFO,
    WARNING,
    ERROR,
    SUCCESS
}

/**
 * Loading Dialog
 *
 * Full-screen loading overlay with message
 * Prevents interaction while loading
 *
 * @param message Loading message
 * @param modifier Modifier
 */
@Composable
fun LoadingDialog(
    message: String = "Loading...",
    modifier: Modifier = Modifier
) {
    Dialog(onDismissRequest = { }) {
        Surface(
            modifier = modifier,
            shape = MaterialTheme.shapes.large,
            color = ColorTokens.ReactTheme.card,
            tonalElevation = Tokens.Elevation.level3
        ) {
            Column(
                modifier = Modifier.padding(Tokens.Spacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.md)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(Tokens.Size.progressLarge)
                )
                Text(
                    text = message,
                    style = TypographyTokens.typography.bodyLarge
                )
            }
        }
    }
}

/**
 * Full Screen Dialog
 *
 * Full-screen modal for complex forms or multi-step flows
 * Has its own top app bar with close button
 *
 * @param title Dialog title
 * @param onDismiss Dismiss handler
 * @param modifier Modifier
 * @param actions Optional top bar actions
 * @param content Dialog content
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenDialog(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    actions: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = title,
                            style = TypographyTokens.typography.titleLarge
                        )
                    },
                    navigationIcon = {
                        IconButtonStandard(
                            icon = IconSet.Navigation.close,
                            onClick = onDismiss,
                            contentDescription = "Close"
                        )
                    },
                    actions = {
                        if (actions != null) {
                            Row(content = actions)
                        }
                    }
                )
            },
            content = content
        )
    }
}

/**
 * Input Dialog
 *
 * Simple dialog with text input
 * Common for quick text entry (name, title, etc.)
 *
 * @param title Dialog title
 * @param onConfirm Confirm handler with input value
 * @param onDismiss Dismiss handler
 * @param modifier Modifier
 * @param initialValue Initial input value
 * @param label Input field label
 * @param placeholder Input placeholder
 * @param confirmText Confirm button text
 * @param dismissText Dismiss button text
 */
@Composable
fun InputDialog(
    title: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    initialValue: String = "",
    label: String = "Enter value",
    placeholder: String = "",
    confirmText: String = "OK",
    dismissText: String = "Cancel"
) {
    var inputValue by remember { mutableStateOf(initialValue) }

    KosmosDialogSurface(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Text(
            text = title,
            style = KosmosDialogDefaults.titleStyle,
            color = KosmosDialogDefaults.titleColor
        )

        OutlinedTextField(
            value = inputValue,
            onValueChange = { inputValue = it },
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = KosmosDialogDefaults.textFieldColors(),
            shape = RoundedCornerShape(Tokens.CornerRadius.md)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Tokens.Spacing.sm)
        ) {
            SecondaryButton(
                text = dismissText,
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            )
            PrimaryButton(
                text = confirmText,
                onClick = { onConfirm(inputValue) },
                enabled = inputValue.isNotBlank(),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * List Selection Dialog
 *
 * Dialog with list of options to select from
 * Alternative to dropdown for better mobile UX
 *
 * @param title Dialog title
 * @param options List of options
 * @param onOptionSelected Option selection handler
 * @param onDismiss Dismiss handler
 * @param modifier Modifier
 * @param selectedOption Currently selected option
 */
@Composable
fun <T> ListSelectionDialog(
    title: String,
    options: List<T>,
    onOptionSelected: (T) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    selectedOption: T? = null,
    optionLabel: (T) -> String = { it.toString() }
) {
    KosmosDialogSurface(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Text(
            text = title,
            style = KosmosDialogDefaults.titleStyle,
            color = KosmosDialogDefaults.titleColor
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Tokens.Spacing.xs)
        ) {
            options.forEach { option ->
                val isSelected = option == selectedOption
                Surface(
                    onClick = {
                        onOptionSelected(option)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Tokens.CornerRadius.md),
                    color = if (isSelected)
                        ColorTokens.ReactTheme.secondary
                    else
                        ColorTokens.ReactTheme.card
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Tokens.Spacing.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = optionLabel(option),
                            style = TypographyTokens.typography.bodyLarge,
                            color = ColorTokens.ReactTheme.foreground,
                            modifier = Modifier.weight(1f)
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = IconSet.Status.checkmark,
                                contentDescription = "Selected",
                                tint = ColorTokens.ReactTheme.primary
                            )
                        }
                    }
                }
            }
        }

        SecondaryButton(
            text = "Cancel",
            onClick = onDismiss,
            fullWidth = true
        )
    }
}

/**
 * Date Picker Dialog
 *
 * Material 3 date picker in dialog
 *
 * @param onDateSelected Date selection handler (timestamp in millis)
 * @param onDismiss Dismiss handler
 * @param modifier Modifier
 * @param initialDate Initial selected date (timestamp in millis)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialog(
    onDateSelected: (Long?) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    initialDate: Long? = null
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate
    )

    androidx.compose.material3.DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButtonStandard(
                text = "OK",
                onClick = {
                    onDateSelected(datePickerState.selectedDateMillis)
                    onDismiss()
                }
            )
        },
        dismissButton = {
            TextButtonStandard(
                text = "Cancel",
                onClick = onDismiss
            )
        },
        modifier = modifier,
        colors = androidx.compose.material3.DatePickerDefaults.colors(
            containerColor = ColorTokens.ReactTheme.card
        )
    ) {
                DatePicker(
                    state = datePickerState,
                    showModeToggle = true,
                    colors = androidx.compose.material3.DatePickerDefaults.colors(
                        containerColor = ColorTokens.ReactTheme.card,
                        titleContentColor = ColorTokens.ReactTheme.foreground,
                        headlineContentColor = ColorTokens.ReactTheme.foreground,
                        weekdayContentColor = ColorTokens.ReactTheme.mutedForeground,
                        navigationContentColor = ColorTokens.ReactTheme.foreground,
                        yearContentColor = ColorTokens.ReactTheme.foreground,
                        currentYearContentColor = ColorTokens.ReactTheme.primary,
                        selectedYearContainerColor = ColorTokens.ReactTheme.primary,
                        selectedYearContentColor = ColorTokens.ReactTheme.primaryForeground,
                        dayContentColor = ColorTokens.ReactTheme.foreground,
                        selectedDayContainerColor = ColorTokens.ReactTheme.primary,
                        selectedDayContentColor = ColorTokens.ReactTheme.primaryForeground,
                        todayContentColor = ColorTokens.ReactTheme.primary,
                        todayDateBorderColor = ColorTokens.ReactTheme.primary
                    )
                )
    }
}

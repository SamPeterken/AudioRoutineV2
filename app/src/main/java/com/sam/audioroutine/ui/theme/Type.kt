package com.sam.audioroutine.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val EditorialDisplay = FontFamily.Serif
private val EditorialBody = FontFamily.SansSerif

val Typography = Typography(
	displaySmall = TextStyle(
		fontFamily = EditorialDisplay,
		fontWeight = FontWeight.Normal,
		fontSize = 42.sp,
		lineHeight = 44.sp,
		letterSpacing = (-0.6f).sp
	),
	headlineLarge = TextStyle(
		fontFamily = EditorialDisplay,
		fontWeight = FontWeight.Normal,
		fontSize = 32.sp,
		lineHeight = 36.sp,
		letterSpacing = (-0.4f).sp
	),
	headlineMedium = TextStyle(
		fontFamily = EditorialDisplay,
		fontWeight = FontWeight.Normal,
		fontSize = 26.sp,
		lineHeight = 30.sp,
		letterSpacing = (-0.25f).sp
	),
	titleMedium = TextStyle(
		fontFamily = EditorialBody,
		fontWeight = FontWeight.SemiBold,
		fontSize = 15.sp,
		lineHeight = 20.sp,
		letterSpacing = 0.4f.sp
	),
	bodyLarge = TextStyle(
		fontFamily = EditorialBody,
		fontWeight = FontWeight.Normal,
		fontSize = 15.sp,
		lineHeight = 22.sp,
		letterSpacing = 0.15f.sp
	),
	bodyMedium = TextStyle(
		fontFamily = EditorialBody,
		fontWeight = FontWeight.Normal,
		fontSize = 14.sp,
		lineHeight = 20.sp,
		letterSpacing = 0.15f.sp
	),
	labelLarge = TextStyle(
		fontFamily = EditorialBody,
		fontWeight = FontWeight.Medium,
		fontSize = 13.sp,
		lineHeight = 16.sp,
		letterSpacing = 0.6f.sp
	),
	labelSmall = TextStyle(
		fontFamily = EditorialBody,
		fontWeight = FontWeight.Medium,
		fontSize = 11.sp,
		lineHeight = 14.sp,
		letterSpacing = 1.1f.sp
	)
)

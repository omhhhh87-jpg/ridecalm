package com.ridecalm.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.sin

// ───────────── 색 ─────────────
object C {
    val Bg = Color(0xFF080D18)
    val Card = Color(0xFF0F1828)
    val Card2 = Color(0xFF162036)
    val Raised = Color(0xFF1C2842)
    val Line = Color(0x1FFFFFFF)
    val Text = Color(0xFFEDF2FA)
    val Sub = Color(0xFF93A0B8)
    val Dim = Color(0xFF5E6A84)
    val Mint = Color(0xFF5EE6C8)
    val Amber = Color(0xFFFFB547)
    val Coral = Color(0xFFFF6B6B)
    val Sky = Color(0xFF7AB8FF)
    val Violet = Color(0xFFA897FF)
}

fun modeColor(m: CareMode?) = when (m) {
    CareMode.MOTION -> C.Mint
    CareMode.GUARD -> C.Amber
    else -> C.Dim
}

fun levelColor(l: Level) = when (l) {
    Level.INFO -> C.Sky
    Level.CARE -> C.Mint
    Level.DANGER -> C.Coral
}

@Composable
fun T(
    text: String,
    size: TextUnit = 18.sp,
    color: Color = C.Text,
    weight: FontWeight = FontWeight.Normal,
    modifier: Modifier = Modifier,
    align: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
) = Text(
    text, modifier, style = TextStyle(fontSize = size, color = color, fontWeight = weight, letterSpacing = (-0.2).sp),
    textAlign = align, maxLines = maxLines, overflow = TextOverflow.Ellipsis,
)

fun Modifier.tap(onClick: () -> Unit) = this.clickable(interactionSource = null, indication = null, onClick = onClick)

@Composable
fun Glass(
    modifier: Modifier = Modifier,
    padding: Dp = 26.dp,
    glow: Color? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    // 평면 카드: 그라디언트·글로우 없이 바탕색과 얇은 경계선만. 강조가 필요할 때만 경계선 색을 바꾼다.
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier
            .clip(shape)
            .background(C.Card)
            .border(if (glow == C.Coral) 2.dp else 1.dp, if (glow == C.Coral) C.Coral.copy(alpha = 0.7f) else C.Line, shape)
            .padding(padding),
        content = content,
    )
}

@Composable
fun Pill(text: String, color: Color, filled: Boolean = false, modifier: Modifier = Modifier, size: TextUnit = 15.sp) {
    Box(
        modifier
            .clip(CircleShape)
            .background(if (filled) color else color.copy(alpha = 0.13f))
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        T(text, size, if (filled) C.Bg else color, FontWeight.SemiBold)
    }
}

@Composable
fun Dot(color: Color, size: Dp = 10.dp) = Box(Modifier.size(size).clip(CircleShape).background(color))

// ───────────── 앱 ─────────────
@Composable
fun RideCalmApp(e: RideEngine) {
    Box(
        Modifier
            .fillMaxSize()
            .background(C.Bg)
            .drawBehind {
                drawCircle(Brush.radialGradient(listOf(C.Mint.copy(alpha = 0.10f), Color.Transparent), center = Offset(size.width * 0.28f, -size.height * 0.1f), radius = size.width * 0.55f))
                drawCircle(Brush.radialGradient(listOf(C.Violet.copy(alpha = 0.08f), Color.Transparent), center = Offset(size.width * 1.0f, size.height * 1.05f), radius = size.width * 0.5f))
            },
    ) {
        Row(Modifier.fillMaxSize()) {
            NavRail(e)
            Column(Modifier.weight(1f).fillMaxHeight().padding(top = 26.dp, end = 30.dp, bottom = 22.dp)) {
                Header(e)
                Spacer(Modifier.height(22.dp))
                AnimatedContent(e.screen, Modifier.weight(1f), transitionSpec = { fadeIn(tween(350)) togetherWith fadeOut(tween(250)) }, label = "screen") { s ->
                    when (s) {
                        Screen.CARE -> CareScreen(e)
                        Screen.REPORT -> ReportScreen(e)
                    }
                }
                AnimatedVisibility(e.consoleOpen) {
                    Column {
                        Spacer(Modifier.height(18.dp))
                        DemoConsole(e)
                    }
                }
            }
        }
        AlertBanner(e)
    }
}

// ───────────── 왼쪽 메뉴 ─────────────
@Composable
fun NavRail(e: RideEngine) {
    Column(
        Modifier.width(118.dp).fillMaxHeight().padding(vertical = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Logo(Modifier.size(58.dp))
        Spacer(Modifier.height(8.dp))
        T("RideCalm", 13.sp, C.Sub, FontWeight.SemiBold)
        Spacer(Modifier.height(46.dp))
        NavItem("케어", e.screen == Screen.CARE, { IconSeat(it) }) { e.screen = Screen.CARE }
        Spacer(Modifier.height(18.dp))
        NavItem("리포트", e.screen == Screen.REPORT, { IconChart(it) }) { e.screen = Screen.REPORT }
        Spacer(Modifier.weight(1f))
        NavItem("시연", e.consoleOpen, { IconSliders(it) }) { e.consoleOpen = !e.consoleOpen }
    }
}

@Composable
fun NavItem(label: String, selected: Boolean, icon: @Composable (Color) -> Unit, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) C.Mint.copy(alpha = 0.14f) else Color.Transparent, label = "nav")
    val fg = if (selected) C.Mint else C.Sub
    Column(
        Modifier.clip(RoundedCornerShape(22.dp)).background(bg).tap(onClick).padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(38.dp)) { icon(fg) }
        Spacer(Modifier.height(6.dp))
        T(label, 17.sp, fg, FontWeight.SemiBold)
    }
}

@Composable
fun Logo(modifier: Modifier) = Canvas(modifier) {
    drawCircle(Brush.linearGradient(listOf(Color(0xFF1B3A4A), Color(0xFF1A2440))))
    val w = size.width
    fun wave(y: Float, color: Color) {
        val p = Path().apply {
            moveTo(w * 0.22f, y)
            cubicTo(w * 0.35f, y - w * 0.14f, w * 0.45f, y - w * 0.14f, w * 0.5f, y)
            cubicTo(w * 0.55f, y + w * 0.14f, w * 0.65f, y + w * 0.14f, w * 0.78f, y)
        }
        drawPath(p, color, style = Stroke(w * 0.07f, cap = StrokeCap.Round))
    }
    wave(w * 0.44f, C.Mint)
    wave(w * 0.62f, C.Sky.copy(alpha = 0.7f))
}

@Composable
fun IconSeat(c: Color) = Canvas(Modifier.fillMaxSize()) {
    val w = size.width
    val st = Stroke(w * 0.08f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    drawRoundRect(c, Offset(w * 0.28f, w * 0.1f), Size(w * 0.3f, w * 0.52f), CornerRadius(w * 0.12f), style = st)
    drawRoundRect(c, Offset(w * 0.28f, w * 0.58f), Size(w * 0.5f, w * 0.18f), CornerRadius(w * 0.08f), style = st)
    drawLine(c, Offset(w * 0.4f, w * 0.78f), Offset(w * 0.4f, w * 0.92f), w * 0.08f, StrokeCap.Round)
    drawLine(c, Offset(w * 0.68f, w * 0.78f), Offset(w * 0.68f, w * 0.92f), w * 0.08f, StrokeCap.Round)
}

@Composable
fun IconChart(c: Color) = Canvas(Modifier.fillMaxSize()) {
    val w = size.width
    listOf(0.55f, 0.3f, 0.7f).forEachIndexed { i, h ->
        val x = w * (0.22f + i * 0.28f)
        drawLine(c, Offset(x, w * 0.88f), Offset(x, w * (0.88f - h * 0.8f)), w * 0.13f, StrokeCap.Round)
    }
}

@Composable
fun IconSliders(c: Color) = Canvas(Modifier.fillMaxSize()) {
    val w = size.width
    listOf(0.25f to 0.62f, 0.5f to 0.32f, 0.75f to 0.55f).forEach { (y, x) ->
        drawLine(c.copy(alpha = 0.5f), Offset(w * 0.12f, w * y), Offset(w * 0.88f, w * y), w * 0.07f, StrokeCap.Round)
        drawCircle(c, w * 0.1f, Offset(w * x, w * y))
    }
}

// ───────────── 상단 ─────────────
@Composable
fun Header(e: RideEngine) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            T(
                when {
                    e.screen == Screen.REPORT -> if (e.layout == Layout.CARE) "이번 주행 돌봄 리포트" else "오늘의 동승자 편안 리포트"
                    e.inTrip && e.layout == Layout.CARE -> "뒷좌석을 지켜보는 중이에요"
                    e.inTrip || e.moving -> "편안하게 모시는 중이에요"
                    else -> "누가 함께 타나요?"
                },
                40.sp, C.Text, FontWeight.Bold,
            )
            Spacer(Modifier.height(6.dp))
            T(e.careSummary(), 20.sp, C.Sub)
        }
        Pill(if (e.vehicleLinked) "차량 연결됨" else "시뮬레이션", if (e.vehicleLinked) C.Mint else C.Sub)
        Spacer(Modifier.width(18.dp))
        GearStrip(e.gear)
        Spacer(Modifier.width(24.dp))
        ScoreRing(e.comfort, Modifier.size(64.dp), 6.dp, 20.sp)
        Spacer(Modifier.width(10.dp))
        Column {
            T("편안 점수", 14.sp, C.Dim)
            T(if (e.layout == Layout.CARE) "돌봄 알림 ${e.guardEvents}회" else "미리 알림 ${e.preAlerts}회", 16.sp, C.Sub, FontWeight.SemiBold)
        }
        Spacer(Modifier.width(24.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            T("${e.speed.toInt()}", 52.sp, C.Text, FontWeight.Bold)
            Spacer(Modifier.width(6.dp))
            T("km/h", 18.sp, C.Sub, modifier = Modifier.padding(bottom = 8.dp))
        }
        Spacer(Modifier.width(22.dp))
        Column(horizontalAlignment = Alignment.End) {
            T("주행", 14.sp, C.Dim)
            T(fmtTime(e.tripSec), 22.sp, C.Text, FontWeight.SemiBold)
        }
    }
}

@Composable
fun GearStrip(gear: String) {
    Row(Modifier.clip(RoundedCornerShape(16.dp)).background(C.Card2).padding(6.dp)) {
        listOf("P", "R", "N", "D").forEach { g ->
            val on = g == gear
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(if (on) C.Text else Color.Transparent),
                contentAlignment = Alignment.Center,
            ) { T(g, 18.sp, if (on) C.Bg else C.Dim, FontWeight.Bold) }
        }
    }
}

fun fmtTime(s: Int) = "%d:%02d".format(s / 60, s % 60)

// ───────────── 케어 화면 ─────────────
@Composable
fun CareScreen(e: RideEngine) {
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(22.dp)) {
        SeatCard(e, Modifier.width(380.dp).fillMaxHeight())
        AnimatedContent(e.layout, Modifier.weight(1f).fillMaxHeight(), transitionSpec = { fadeIn(tween(350)) togetherWith fadeOut(tween(200)) }, label = "layout") { l ->
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                when (l) {
                    Layout.MOTION -> {
                        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(22.dp)) {
                            MotionHero(e, Modifier.fillMaxWidth().weight(1f))
                            SelfCare(e, Modifier.fillMaxWidth())
                        }
                        Column(Modifier.width(360.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(22.dp)) {
                            CabinCard(e, Modifier.fillMaxWidth().weight(1.05f))
                            MotionTipCard(e, Modifier.fillMaxWidth().weight(1f))
                        }
                    }
                    Layout.CARE -> {
                        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(22.dp)) {
                            CareHero(e, Modifier.fillMaxWidth().weight(1f))
                            DriverCare(e, Modifier.fillMaxWidth())
                        }
                        CareLogCard(e, Modifier.width(360.dp).fillMaxHeight())
                    }
                    Layout.BOTH -> {
                        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(22.dp)) {
                            MotionHero(e, Modifier.fillMaxWidth().weight(1f))
                            SelfCare(e, Modifier.fillMaxWidth())
                        }
                        Column(Modifier.width(360.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(22.dp)) {
                            CabinCard(e, Modifier.fillMaxWidth().weight(1.05f))
                            GuardCard(e, Modifier.fillMaxWidth().weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionTitle(title: String, sub: String? = null, trailing: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            T(title, 26.sp, C.Text, FontWeight.Bold)
            if (sub != null) {
                Spacer(Modifier.height(4.dp))
                T(sub, 17.sp, C.Sub)
            }
        }
        trailing?.invoke()
    }
}

// 좌석 배치도
@Composable
fun SeatCard(e: RideEngine, modifier: Modifier) {
    Glass(modifier) {
        SectionTitle("누구를 케어할까요?", "정차 중에만 바꿀 수 있어요")
        Spacer(Modifier.height(18.dp))
        LayoutPicker(e)
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            CarTopView(e)
        }
        if (e.guardSeats.isNotEmpty()) {
            TargetPicker(e)
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(C.Card2).tap { e.togglePassengerVoice() }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    T("동승자 안내 음성", 16.sp, C.Text, FontWeight.SemiBold)
                    T(if (e.passengerVoice) "쉬운 말로 안내해요" else "말 대신 운전자에게만 알려요", 13.sp, C.Sub)
                }
                Pill(if (e.passengerVoice) "켜짐" else "꺼짐", if (e.passengerVoice) C.Mint else C.Sub)
            }
            Spacer(Modifier.height(10.dp))
        }
        T("좌석을 누르면 좌석마다 따로 정할 수 있어요", 15.sp, C.Sub)
        if (e.motionSeats.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                T("멀미 민감도", 16.sp, C.Sub, modifier = Modifier.weight(1f))
                Segmented(listOf("약", "중", "강"), e.sensitivity) { if (!e.moving) e.sensitivity = it }
            }
        }
    }
}

// 돌봄 대상: 진단명 대신 연령대 아이콘으로 고른다
@Composable
fun TargetPicker(e: RideEngine) {
    T("누가 타나요?", 17.sp, C.Text, FontWeight.SemiBold)
    Spacer(Modifier.height(8.dp))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CareTarget.entries.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { t ->
                    val on = t == e.careTarget
                    val shape = RoundedCornerShape(12.dp)
                    Row(
                        Modifier.weight(1f).clip(shape)
                            .background(if (on) C.Amber.copy(alpha = 0.16f) else C.Card2)
                            .border(if (on) 2.dp else 1.dp, if (on) C.Amber else C.Line, shape)
                            .tap { e.selectTarget(t) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        T(t.emoji, 30.sp)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            T(t.label, 17.sp, if (on) C.Text else C.Sub, FontWeight.Bold, maxLines = 1)
                            T(t.age, 14.sp, if (on) C.Amber else C.Dim, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LayoutPicker(e: RideEngine) {
    val sel = if (e.motionSeats.isEmpty() && e.guardSeats.isEmpty()) null else e.layout
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(C.Bg.copy(alpha = 0.5f)).padding(5.dp)) {
        Layout.entries.forEach { l ->
            val on = l == sel
            val col = when (l) {
                Layout.MOTION -> C.Mint
                Layout.CARE -> C.Amber
                Layout.BOTH -> C.Text
            }
            val bg by animateColorAsState(if (on) col else Color.Transparent, label = "pick")
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(bg).tap { e.selectLayout(l) }.padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) { T(l.label, 19.sp, if (on) C.Bg else col.copy(alpha = 0.8f), FontWeight.Bold) }
        }
    }
}

@Composable
fun Segmented(items: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.clip(RoundedCornerShape(14.dp)).background(C.Bg.copy(alpha = 0.5f)).padding(4.dp)) {
        items.forEachIndexed { i, s ->
            val on = i == selected
            Box(
                Modifier.clip(RoundedCornerShape(11.dp)).background(if (on) C.Mint else Color.Transparent).tap { onSelect(i) }
                    .padding(horizontal = 18.dp, vertical = 7.dp),
            ) { T(s, 16.sp, if (on) C.Bg else C.Sub, FontWeight.SemiBold) }
        }
    }
}

@Composable
fun CarTopView(e: RideEngine) {
    val w = 310.dp
    val h = 540.dp
    Box(Modifier.size(w, h)) {
        Canvas(Modifier.fillMaxSize()) {
            val cw = size.width
            val ch = size.height
            // 차체
            drawRoundRect(
                Brush.verticalGradient(listOf(Color(0xFF2A3754), Color(0xFF1B2539))),
                Offset(cw * 0.06f, ch * 0.02f), Size(cw * 0.88f, ch * 0.96f), CornerRadius(cw * 0.36f),
            )
            drawRoundRect(C.Line, Offset(cw * 0.06f, ch * 0.02f), Size(cw * 0.88f, ch * 0.96f), CornerRadius(cw * 0.36f), style = Stroke(2f))
            // 앞유리 / 뒷유리
            val front = Path().apply {
                moveTo(cw * 0.2f, ch * 0.2f); lineTo(cw * 0.8f, ch * 0.2f)
                lineTo(cw * 0.72f, ch * 0.12f); lineTo(cw * 0.28f, ch * 0.12f); close()
            }
            drawPath(front, Color(0xFF3B5276).copy(alpha = 0.8f))
            val rear = Path().apply {
                moveTo(cw * 0.24f, ch * 0.86f); lineTo(cw * 0.76f, ch * 0.86f)
                lineTo(cw * 0.7f, ch * 0.92f); lineTo(cw * 0.3f, ch * 0.92f); close()
            }
            drawPath(rear, Color(0xFF3B5276).copy(alpha = 0.6f))
            // 사이드미러
            drawOval(Color(0xFF2A3754), Offset(0f, ch * 0.2f), Size(cw * 0.1f, ch * 0.04f))
            drawOval(Color(0xFF2A3754), Offset(cw * 0.9f, ch * 0.2f), Size(cw * 0.1f, ch * 0.04f))
            // 전조등
            drawLine(C.Sky.copy(alpha = 0.6f), Offset(cw * 0.22f, ch * 0.035f), Offset(cw * 0.36f, ch * 0.03f), 6f, StrokeCap.Round)
            drawLine(C.Sky.copy(alpha = 0.6f), Offset(cw * 0.64f, ch * 0.03f), Offset(cw * 0.78f, ch * 0.035f), 6f, StrokeCap.Round)
        }
        val sw = 108.dp
        val sh = 136.dp
        SeatChip(e, Seat.DRIVER, Modifier.offset(36.dp, 140.dp).size(sw, sh))
        SeatChip(e, Seat.PASSENGER, Modifier.offset(166.dp, 140.dp).size(sw, sh))
        SeatChip(e, Seat.REAR_L, Modifier.offset(36.dp, 312.dp).size(sw, sh))
        SeatChip(e, Seat.REAR_R, Modifier.offset(166.dp, 312.dp).size(sw, sh))
        T("앞", 15.sp, C.Dim, modifier = Modifier.align(Alignment.TopCenter).padding(top = 76.dp))
    }
}

@Composable
fun SeatChip(e: RideEngine, seat: Seat, modifier: Modifier) {
    val mode = e.seatModes[seat]
    val col = modeColor(mode)
    val active = mode != CareMode.NONE
    val bg by animateColorAsState(if (active) col.copy(alpha = 0.16f) else C.Card2, label = "seat")
    val win = e.windowOpen[seat] ?: 0
    val beltOk = e.belt[seat] != false
    val alarm = win > 0 && seat.rear && mode == CareMode.GUARD || !beltOk || e.doorOpen[seat] == true || mode == CareMode.GUARD && e.occupied[seat] == false
    val alarmCol = if (e.inTrip) C.Coral else C.Amber
    Box(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (alarm) alarmCol.copy(alpha = 0.22f) else bg)
            .border(if (alarm) 3.dp else 2.dp, if (alarm) alarmCol else if (active) col else Color.Transparent, RoundedCornerShape(16.dp))
            .tap { e.cycleSeat(seat) }
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            T(seat.short, 17.sp, if (active) C.Text else C.Sub, FontWeight.SemiBold, align = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            if (mode == CareMode.GUARD) {
                T(e.careTarget.emoji, 34.sp)
                T(e.careTarget.label, 16.sp, col, FontWeight.Bold, maxLines = 1)
            } else {
                T(
                    when (mode) {
                        CareMode.MOTION -> "멀미"
                        else -> if (seat == Seat.DRIVER) "운전 팁" else "—"
                    },
                    21.sp, if (active) col else C.Dim, FontWeight.Bold,
                )
            }
            if (win > 0) {
                Spacer(Modifier.height(2.dp))
                T("창문 $win%", 15.sp, if (alarm) alarmCol else C.Sky, FontWeight.SemiBold)
            }
        }
    }
}

// 흔들림 미리 알림
@Composable
fun MotionHero(e: RideEngine, modifier: Modifier) {
    val m = e.maneuver
    val accent = when (m?.kind) {
        ManeuverKind.BRAKE -> C.Coral
        ManeuverKind.BUMP -> C.Amber
        ManeuverKind.TUNNEL -> C.Violet
        null -> C.Mint
        else -> C.Sky
    }
    val pulse = rememberInfiniteTransition(label = "edge")
    val edge by pulse.animateFloat(0.3f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "edge")
    Glass(
        modifier.drawBehind {
            // 회전 방향 쪽 실내조명(앰비언트) 표현
            if (m?.kind == ManeuverKind.RIGHT || m?.kind == ManeuverKind.LEFT) {
                val x = if (m.kind == ManeuverKind.RIGHT) size.width - 10f else 0f
                drawRoundRect(
                    Brush.verticalGradient(listOf(Color.Transparent, accent.copy(alpha = edge), Color.Transparent)),
                    Offset(x, size.height * 0.08f), Size(10f, size.height * 0.84f), CornerRadius(5f),
                )
            }
        },
        glow = accent,
    ) {
        SectionTitle("흔들림 미리 알림", "내비 경로로 회전·제동을 몇 초 먼저 알려요") {
            Pill("다음 · ${e.nextHint}", C.Sub)
        }
        AnimatedContent(m, Modifier.fillMaxWidth().weight(1f), transitionSpec = {
            (fadeIn(tween(300)) + slideInVertically { it / 8 }) togetherWith fadeOut(tween(200))
        }, label = "hero") { cur ->
            if (cur == null) IdleHero(e) else ManeuverView(e, cur, accent)
        }
    }
}

@Composable
fun ManeuverView(e: RideEngine, m: Maneuver, accent: Color) {
    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(190.dp).clip(CircleShape).background(Brush.radialGradient(listOf(accent.copy(alpha = 0.18f), Color.Transparent))),
            contentAlignment = Alignment.Center,
        ) {
            ManeuverGlyph(m.kind, accent, Modifier.size(140.dp))
        }
        Spacer(Modifier.width(26.dp))
        Column(Modifier.weight(1f)) {
            T(m.title, 50.sp, C.Text, FontWeight.Bold, maxLines = 1)
            Spacer(Modifier.height(10.dp))
            T(m.detail, 24.sp, accent, FontWeight.Medium)
            Spacer(Modifier.height(28.dp))
            val p = (e.maneuverLeft / m.totalSec).coerceIn(0f, 1f)
            Box(Modifier.fillMaxWidth().height(10.dp).clip(CircleShape).background(C.Raised)) {
                Box(Modifier.fillMaxWidth(1f - p).fillMaxHeight().clip(CircleShape).background(Brush.horizontalGradient(listOf(accent.copy(alpha = 0.5f), accent))))
            }
            Spacer(Modifier.height(12.dp))
            Row {
                T("약 ${kotlin.math.ceil(e.maneuverLeft).toInt()}초 후", 20.sp, C.Sub, modifier = Modifier.weight(1f))
                T("실내조명 · ${e.ambient}", 20.sp, C.Sub)
            }
        }
    }
}

@Composable
fun IdleHero(e: RideEngine) {
    val t = rememberInfiniteTransition(label = "wave")
    val ph by t.animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(4000, easing = LinearEasing)), label = "ph")
    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            val amp = if (e.moving) size.height * 0.05f else size.height * 0.025f
            listOf(0.46f to C.Mint, 0.54f to C.Sky).forEachIndexed { i, (y, col) ->
                val p = Path()
                var x = 0f
                p.moveTo(0f, size.height * y)
                while (x <= size.width) {
                    p.lineTo(x, size.height * y + amp * sin(x / size.width * 4 * PI.toFloat() + ph + i).toFloat())
                    x += 8f
                }
                drawPath(p, col.copy(alpha = 0.45f - i * 0.15f), style = Stroke(5f, cap = StrokeCap.Round))
            }
        }
        Column(Modifier.align(Alignment.TopStart).padding(top = 28.dp)) {
            T(if (e.moving) "지금은 부드럽게 달리는 중" else "출발 준비 완료", 56.sp, C.Text, FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            T(
                if (e.moving) "흔들림이 오기 전에 목소리와 실내조명으로 먼저 알려 드릴게요."
                else "출발하면 회전·과속방지턱·급제동을 미리 알려 드려요.",
                24.sp, C.Sub,
            )
        }
        RoutePreview(Modifier.align(Alignment.BottomStart).fillMaxWidth())
    }
}

// 경로의 흔들림 구간 미리보기 (내비 경로 기반, 시연용 경로)
@Composable
fun RoutePreview(modifier: Modifier) {
    val items = listOf(
        Triple("300m", "우회전", C.Sky), Triple("1.2km", "과속방지턱", C.Amber),
        Triple("2.0km", "터널", C.Violet), Triple("2.6km", "좌회전", C.Sky),
    )
    Column(modifier) {
        T("경로의 흔들림 구간", 17.sp, C.Dim, FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            items.forEach { (dist, label, col) ->
                Row(
                    Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).background(C.Bg.copy(alpha = 0.45f))
                        .border(1.dp, col.copy(alpha = 0.25f), RoundedCornerShape(20.dp)).padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Dot(col, 12.dp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        T(label, 18.sp, C.Text, FontWeight.SemiBold, maxLines = 1)
                        T(dist, 16.sp, col)
                    }
                }
            }
        }
    }
}

@Composable
fun ManeuverGlyph(kind: ManeuverKind, color: Color, modifier: Modifier) {
    val t = rememberInfiniteTransition(label = "glyph")
    val ph by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1100, easing = LinearEasing)), label = "ph")
    Canvas(modifier) {
        val w = size.width
        val stroke = w * 0.11f
        when (kind) {
            ManeuverKind.RIGHT, ManeuverKind.LEFT -> scale(if (kind == ManeuverKind.LEFT) -1f else 1f, 1f) {
                val path = Path().apply {
                    moveTo(w * 0.34f, w * 0.92f)
                    lineTo(w * 0.34f, w * 0.56f)
                    cubicTo(w * 0.34f, w * 0.40f, w * 0.44f, w * 0.34f, w * 0.58f, w * 0.34f)
                    lineTo(w * 0.64f, w * 0.34f)
                }
                drawPath(path, color.copy(alpha = 0.22f), style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawPath(
                    path, color,
                    style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(w * 0.05f, w * 0.11f), -ph * w * 0.16f)),
                )
                val head = Path().apply {
                    moveTo(w * 0.62f, w * 0.16f); lineTo(w * 0.9f, w * 0.34f); lineTo(w * 0.62f, w * 0.52f); close()
                }
                drawPath(head, color)
            }
            ManeuverKind.BUMP -> {
                drawLine(color.copy(alpha = 0.35f), Offset(w * 0.05f, w * 0.72f), Offset(w * 0.95f, w * 0.72f), w * 0.05f, StrokeCap.Round)
                val bump = Path().apply {
                    moveTo(w * 0.22f, w * 0.72f)
                    cubicTo(w * 0.36f, w * 0.36f, w * 0.64f, w * 0.36f, w * 0.78f, w * 0.72f)
                }
                drawPath(bump, color, style = Stroke(stroke, cap = StrokeCap.Round))
                val y = w * (0.3f - ph * 0.08f)
                drawLine(color, Offset(w * 0.5f, y), Offset(w * 0.5f, y - w * 0.14f), w * 0.06f, StrokeCap.Round)
            }
            ManeuverKind.TUNNEL -> {
                val arch = Path().apply {
                    moveTo(w * 0.12f, w * 0.85f); lineTo(w * 0.12f, w * 0.5f)
                    cubicTo(w * 0.12f, w * 0.12f, w * 0.88f, w * 0.12f, w * 0.88f, w * 0.5f)
                    lineTo(w * 0.88f, w * 0.85f)
                }
                drawPath(arch, color, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawLine(color.copy(alpha = 0.4f + ph * 0.6f), Offset(w * 0.5f, w * 0.82f), Offset(w * 0.5f, w * 0.52f), w * 0.05f, StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(w * 0.06f, w * 0.06f)))
            }
            ManeuverKind.BRAKE -> {
                drawCircle(color, w * 0.4f, style = Stroke(stroke))
                drawLine(color, Offset(w * 0.5f, w * 0.28f), Offset(w * 0.5f, w * 0.56f), stroke, StrokeCap.Round)
                drawCircle(color, stroke * 0.6f, Offset(w * 0.5f, w * 0.72f))
            }
        }
    }
}

// 동승자가 직접 누르는 버튼
@Composable
fun SelfCare(e: RideEngine, modifier: Modifier) {
    Row(modifier.height(160.dp), horizontalArrangement = Arrangement.spacedBy(22.dp)) {
        BigAction("속이 안 좋아요", "찬바람 · 휴게소 ${"%.1f".format(e.restKm)}km", C.Mint, Modifier.weight(1f).fillMaxHeight()) { e.sick() }
        BigAction("냄새가 나요", "강한 환기 20초", C.Sky, Modifier.weight(1f).fillMaxHeight()) { e.smell() }
    }
}

@Composable
fun BigAction(title: String, sub: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier.clip(shape)
            .background(C.Card2)
            .border(1.5.dp, color.copy(alpha = 0.45f), shape)
            .tap(onClick)
            .padding(horizontal = 28.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(64.dp).clip(CircleShape).background(color.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(36.dp)) {
                val w = size.width
                val p = Path().apply {
                    moveTo(0f, w * 0.5f)
                    cubicTo(w * 0.25f, w * 0.15f, w * 0.4f, w * 0.15f, w * 0.5f, w * 0.5f)
                    cubicTo(w * 0.6f, w * 0.85f, w * 0.75f, w * 0.85f, w, w * 0.5f)
                }
                drawPath(p, color, style = Stroke(w * 0.14f, cap = StrokeCap.Round))
            }
        }
        Spacer(Modifier.width(20.dp))
        Column {
            T(title, 30.sp, C.Text, FontWeight.Bold, maxLines = 1)
            Spacer(Modifier.height(6.dp))
            T(sub, 18.sp, color, maxLines = 1)
        }
    }
}

@Composable
fun ScoreRing(score: Int, modifier: Modifier, stroke: Dp, textSize: TextUnit) {
    val anim by animateFloatAsState(score / 100f, tween(900), label = "ring")
    val col = when {
        score >= 85 -> C.Mint
        score >= 70 -> C.Amber
        else -> C.Coral
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val s = stroke.toPx()
            val d = size.minDimension - s
            val tl = Offset(s / 2, s / 2)
            drawArc(C.Raised, -90f, 360f, false, tl, Size(d, d), style = Stroke(s))
            drawArc(Brush.sweepGradient(listOf(col.copy(alpha = 0.5f), col, col.copy(alpha = 0.5f))), -90f, 360f * anim, false, tl, Size(d, d), style = Stroke(s, cap = StrokeCap.Round))
        }
        T("$score", textSize, C.Text, FontWeight.Bold)
    }
}

// 실내 환경
@Composable
fun CabinCard(e: RideEngine, modifier: Modifier) {
    Glass(modifier, glow = if (e.ventBoost) C.Sky else null) {
        SectionTitle("실내 환경", "Vehicle SDK 공조·조명 제어 · 지금은 시연") { Pill("시연", C.Violet, size = 15.sp) }
        Spacer(Modifier.height(16.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.SpaceEvenly) {
            EnvRow("공기", if (e.freshAir) "바깥 공기" else "실내 순환", e.freshAir)
            EnvRow("공기청정", if (e.purifier) "켜짐" else "꺼짐", e.purifier)
            EnvRow("온도", "%.1f℃".format(e.tempC), e.tempC < 24f)
            EnvRow("풍량", if (e.ventBoost) "최대 · 환기 중" else "${e.fan}단", e.fan > 2 || e.ventBoost)
            EnvRow("통풍시트", if (e.seatVent > 0) "${e.seatVent}단" else "꺼짐", e.seatVent > 0)
            EnvRow("무드조명", e.ambient, e.ambient != "기본")
        }
    }
}

@Composable
fun EnvRow(label: String, value: String, on: Boolean) {
    val col by animateColorAsState(if (on) C.Mint else C.Sub, tween(400), label = "env")
    Row(verticalAlignment = Alignment.CenterVertically) {
        Dot(if (on) C.Mint else C.Raised, 9.dp)
        Spacer(Modifier.width(12.dp))
        T(label, 20.sp, C.Sub, modifier = Modifier.weight(1f))
        T(value, 21.sp, col, FontWeight.SemiBold)
    }
}

// 멀미만 선택했을 때 오른쪽 아래: 오늘 멀미 상태와 동승자 팁
@Composable
fun MotionTipCard(e: RideEngine, modifier: Modifier) {
    val seat = e.motionSeats.firstOrNull() ?: Seat.PASSENGER
    Glass(modifier, glow = C.Mint) {
        SectionTitle("멀미 케어", "${seat.label} · 민감도 ${listOf("약", "중", "강")[e.sensitivity]}")
        Spacer(Modifier.height(14.dp))
        RestRow(e)
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            T("${e.preAlerts}", 44.sp, C.Mint, FontWeight.Bold)
            T(" 미리 알림", 18.sp, C.Sub, modifier = Modifier.padding(bottom = 8.dp))
            Spacer(Modifier.width(22.dp))
            T("${e.symptoms}", 44.sp, if (e.symptoms > 0) C.Amber else C.Mint, FontWeight.Bold)
            T(" 증상", 18.sp, C.Sub, modifier = Modifier.padding(bottom = 8.dp))
        }
        Spacer(Modifier.height(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.SpaceEvenly) {
            listOf("창밖 먼 곳을 바라봐요", "휴대폰은 잠시 내려놔요", "등받이에 기대 머리를 고정해요").forEach {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Dot(C.Mint.copy(alpha = 0.6f), 8.dp)
                    Spacer(Modifier.width(12.dp))
                    T(it, 19.sp, C.Sub, maxLines = 1)
                }
            }
        }
    }
}

// 가까운 휴게소 + 경유지 추가(운전자가 누름)
@Composable
fun RestRow(e: RideEngine) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(C.Card2).border(1.dp, C.Line, shape).padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            T("멈출 곳 · 가까운 휴게소", 13.sp, C.Sub, maxLines = 1)
            T(e.restShort(), 20.sp, C.Text, FontWeight.SemiBold, maxLines = 1)
        }
        Box(
            Modifier.clip(RoundedCornerShape(10.dp)).background(C.Mint.copy(alpha = 0.16f)).tap { e.addRestStop() }
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) { T("경유지 추가", 16.sp, C.Mint, FontWeight.Bold) }
    }
}

// 돌봄 (둘 다 선택했을 때의 요약 카드)
@Composable
fun GuardCard(e: RideEngine, modifier: Modifier) {
    val seat = e.guardSeats.firstOrNull()
    Glass(modifier, glow = if (seat != null) C.Amber else null) {
        SectionTitle("돌봄", seat?.label ?: "지정된 좌석 없음")
        Spacer(Modifier.height(16.dp))
        if (seat == null) {
            T("좌석을 두 번 눌러 돌봄으로 바꾸면 문·창문·안전벨트를 지켜볼게요.", 17.sp, C.Sub)
            return@Glass
        }
        val win = e.windowOpen[seat] ?: 0
        val beltOk = e.belt[seat] != false
        val door = e.doorOpen[seat] == true
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.SpaceEvenly) {
            GuardRow("문", if (door) "열림" else if (e.doorLock) "닫힘 · 잠김" else "닫힘", if (door) C.Coral else if (e.doorLock) C.Mint else C.Sub)
            GuardRow("창문", if (win > 0) "열림 $win%" else if (e.windowLock) "닫힘 · 잠금" else "닫힘", if (win > 0) C.Coral else C.Mint)
            GuardRow("차일드락", if (e.childLockAsked) "확인 안내함" else "출발 때 안내", if (e.childLockAsked) C.Mint else C.Sub)
            GuardRow("안전벨트", if (beltOk) "착용" else "풀림", if (beltOk) C.Mint else C.Coral)
        }
    }
}

// 돌봄만 선택했을 때 가운데 큰 화면
@Composable
fun CareHero(e: RideEngine, modifier: Modifier) {
    val seat = e.guardSeats.firstOrNull() ?: Seat.REAR_R
    val win = e.windowOpen[seat] ?: 0
    val beltOk = e.belt[seat] != false
    val door = e.doorOpen[seat] == true
    val away = e.occupied[seat] == false
    val checks = e.preCheck()
    val okCount = checks.count { it.ok }
    val pre = !e.inTrip
    val problem = when {
        e.safetyStop -> "라이드캄을 멈췄어요"
        pre -> null
        door -> "문이 열렸어요"
        win > 0 -> "창문이 열렸어요"
        !beltOk -> "안전벨트가 풀렸어요"
        away -> "자리가 비었어요"
        else -> null
    }
    // 출발 전에 덜 된 항목은 주황(안내), 주행 중 문제는 빨강(경고)
    val warn = if (pre) C.Amber else C.Coral
    Glass(modifier, glow = if (problem != null) C.Coral else null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(64.dp).clip(CircleShape).background(C.Amber.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                T(e.careTarget.emoji, 36.sp)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                T("${e.careTarget.label} · ${seat.label}", 26.sp, C.Text, FontWeight.Bold)
                T(if (pre) "출발 전 점검 · 문 · 창문 · 안전벨트 · 착석" else "도착할 때까지 지켜보고, 바뀌면 바로 알려요", 17.sp, C.Sub)
            }
            Pill(
                if (pre) "정차 중" else if (e.moving) "이동 중" else "신호 대기 · 지켜보는 중",
                if (pre) C.Sub else C.Amber,
            )
        }
        Spacer(Modifier.height(24.dp))
        val head = when {
            problem != null -> problem
            pre && okCount == 4 -> "출발 준비 완료"
            pre -> "${4 - okCount}개를 확인해 주세요"
            else -> "모두 안전해요"
        }
        val headColor = when {
            problem != null -> C.Coral
            pre && okCount < 4 -> C.Amber
            pre -> C.Mint
            else -> C.Text
        }
        Row(verticalAlignment = Alignment.Bottom) {
            T(head, 52.sp, headColor, FontWeight.Bold, maxLines = 1)
            if (pre) {
                Spacer(Modifier.width(16.dp))
                T("$okCount / 4", 26.sp, C.Sub, FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        T(
            when {
                e.safetyStop -> "사고 신호를 받아 차량 기본 안전 기능에 맡겼어요"
                pre && okCount < 4 -> checks.filter { !it.ok }.joinToString(" · ") { it.fix }
                pre -> "문 · 창문 · 안전벨트 · 착석 모두 확인했어요. 출발해도 좋아요"
                door -> if (e.moving) "천천히 멈추고 비상등을 켜 주세요" else "아직 도착 전이에요 · 문을 확인해 주세요"
                win > 0 -> "운전석 스위치로 닫고 창문 잠금 버튼을 켜 주세요"
                !beltOk -> "안전한 곳에 멈춰 벨트를 다시 매 주세요"
                away -> "다른 좌석으로 옮겨 앉았어요 · 안전한 곳에서 확인해 주세요"
                else -> "문 · 창문 · 벨트 · 착석이 바뀌면 바로 알려 드려요"
            },
            22.sp, if (problem != null || (pre && okCount < 4)) headColor else C.Sub,
        )
        if (!pre && !e.safetyStop && (!beltOk || away)) {
            Spacer(Modifier.height(12.dp))
            RestRow(e)
        }
        Spacer(Modifier.height(22.dp))
        Column(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                CareTile("문", if (door) "열림" else "닫힘", !door, warn, Modifier.weight(1f))
                CareTile("창문", if (win > 0) "열림 $win%" else "닫힘", win == 0, warn, Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                CareTile("안전벨트", if (beltOk) "착용" else "풀림", beltOk, warn, Modifier.weight(1f))
                CareTile("착석", if (away) "자리 비움" else "앉아 있음", !away, warn, Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun CareTile(label: String, value: String, ok: Boolean, warn: Color, modifier: Modifier) {
    val col by animateColorAsState(if (ok) C.Mint else warn, tween(300), label = "tile")
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier.fillMaxHeight().clip(shape)
            .background(if (ok) C.Card2 else warn.copy(alpha = 0.14f))
            .border(if (ok) 1.dp else 2.dp, if (ok) C.Line else warn.copy(alpha = 0.8f), shape)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(52.dp).clip(CircleShape).background(col.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
            T(if (ok) "✓" else "!", 28.sp, col, FontWeight.Bold)
        }
        Spacer(Modifier.width(18.dp))
        Column {
            T(label, 20.sp, C.Sub, FontWeight.SemiBold)
            T(value, 38.sp, if (ok) C.Text else col, FontWeight.Bold, maxLines = 1)
        }
    }
}

// 돌봄만 선택했을 때 운전자가 누르는 버튼
@Composable
fun DriverCare(e: RideEngine, modifier: Modifier) {
    Row(modifier.height(160.dp), horizontalArrangement = Arrangement.spacedBy(22.dp)) {
        BigAction("안심 음성 들려주기", "\u201C조금만 기다려요, 곧 도착해요\u201D", C.Amber, Modifier.weight(1f).fillMaxHeight()) { e.reassure() }
        BigAction("다시 잠그기", "누르면 문 잠금 · 창문 잠금을 켜요", C.Mint, Modifier.weight(1f).fillMaxHeight()) { e.relock() }
    }
}

// 돌봄 알림 기록
@Composable
fun CareLogCard(e: RideEngine, modifier: Modifier) {
    val items = e.log.filter { it.level != Level.INFO || it.text.contains("안심") || it.text.contains("출발") }.take(7)
    Glass(modifier) {
        SectionTitle("돌봄 알림", "오늘 ${e.guardEvents}회 · 운전자에게 알린 일")
        Spacer(Modifier.height(18.dp))
        if (items.isEmpty()) {
            T("아직 알림이 없어요. 문·창문이 열리거나 벨트가 풀리면 바로 알려 드릴게요.", 18.sp, C.Sub)
        }
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items.forEach { item ->
                Row(verticalAlignment = Alignment.Top) {
                    Box(Modifier.padding(top = 8.dp)) { Dot(levelColor(item.level), 10.dp) }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        T(item.text, 19.sp, C.Text, FontWeight.Medium, maxLines = 2)
                        T(item.time.substring(0, 5), 14.sp, C.Dim)
                    }
                }
            }
        }
    }
}

@Composable
fun GuardRow(label: String, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Dot(color, 9.dp)
        Spacer(Modifier.width(12.dp))
        T(label, 20.sp, C.Sub, modifier = Modifier.weight(1f))
        T(value, 21.sp, color, FontWeight.SemiBold)
    }
}

// ───────────── 알림 배너 ─────────────
@Composable
fun BoxScope.AlertBanner(e: RideEngine) {
    AnimatedVisibility(
        e.alert != null,
        Modifier.align(Alignment.TopCenter).padding(top = 18.dp),
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
    ) {
        val a = e.alert ?: return@AnimatedVisibility
        val col = levelColor(a.level)
        val danger = a.level == Level.DANGER
        val shape = RoundedCornerShape(20.dp)
        Row(
            Modifier.width(1180.dp)
                .clip(shape)
                // 경고는 눈에 띄되 놀라지 않게: 짙은 와인색 바탕 + 붉은 테두리·왼쪽 띠
                .background(
                    if (danger) Brush.horizontalGradient(listOf(Color(0xFF4A1E2A), Color(0xFF2B1A27)))
                    else Brush.horizontalGradient(listOf(Color(0xFF1E2B47), Color(0xFF17213A))),
                )
                .border(2.dp, if (danger) C.Coral.copy(alpha = 0.75f) else col.copy(alpha = 0.6f), shape)
                .tap { e.dismissAlert() }
                .padding(horizontal = 30.dp, vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(76.dp).clip(CircleShape).background(col.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) { T(if (danger) "!" else "i", 32.sp, col, FontWeight.Bold) }
            Spacer(Modifier.width(24.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (a.tier != null) {
                        Box(
                            Modifier.clip(CircleShape).background(C.Coral).padding(horizontal = 14.dp, vertical = 4.dp),
                        ) { T(a.tier, 18.sp, Color(0xFF2B1A27), FontWeight.Bold) }
                        Spacer(Modifier.width(14.dp))
                    }
                    T(a.title, 38.sp, Color.White, FontWeight.Bold)
                }
                Spacer(Modifier.height(4.dp))
                T(a.detail, 22.sp, if (danger) Color(0xFFE6C9CE) else C.Sub)
                if (a.todo != null) {
                    Spacer(Modifier.height(14.dp))
                    Row(
                        Modifier.clip(RoundedCornerShape(16.dp))
                            .background(col.copy(alpha = 0.16f))
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        T("운전자가 할 일", 17.sp, col, FontWeight.Bold)
                        Spacer(Modifier.width(14.dp))
                        T(a.todo, 23.sp, Color.White, FontWeight.Bold)
                    }
                }
                if (a.actions.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        T("앱이 한 일", 16.sp, C.Dim)
                        Spacer(Modifier.width(4.dp))
                        a.actions.forEach {
                            Box(
                                Modifier.clip(CircleShape)
                                    .background(Color(0x14FFFFFF))
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                            ) { T("✓ $it", 16.sp, C.Sub, FontWeight.SemiBold) }
                        }
                    }
                }
            }
        }
    }
}

// ───────────── 시연 콘솔 ─────────────
@Composable
fun DemoConsole(e: RideEngine) {
    val shape = RoundedCornerShape(24.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(C.Card.copy(alpha = 0.9f)).border(1.dp, C.Line, shape).padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.width(104.dp)) {
            T("시연 콘솔", 17.sp, C.Text, FontWeight.Bold)
            T("가상 차량", 14.sp, C.Dim)
        }
        Row(
            Modifier.weight(1f).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ConsoleBtn("출발", C.Text) { e.startDrive() }
            ConsoleBtn("우회전", C.Sky) { e.turn(true) }
            ConsoleBtn("좌회전", C.Sky) { e.turn(false) }
            ConsoleBtn("과속방지턱", C.Amber) { e.bump() }
            ConsoleBtn("터널", C.Violet) { e.tunnel() }
            ConsoleBtn("급제동", C.Coral) { e.brake() }
            ConsoleSep()
            ConsoleBtn("속이 안 좋아", C.Mint) { e.sick() }
            ConsoleBtn("냄새", C.Mint) { e.smell() }
            ConsoleSep()
            ConsoleBtn("창문 열림", C.Amber) { e.rearWindow() }
            ConsoleBtn("창문 닫기(운전자)", C.Sub) { e.driverClosesWindow() }
            ConsoleBtn("문 열림", C.Amber) { e.doorOpened() }
            ConsoleBtn("벨트", C.Amber) { e.beltOff() }
            ConsoleBtn("자리 이동", C.Amber) { e.seatMove() }
            ConsoleBtn("에어백", C.Coral) { e.airbagDeployed() }
            ConsoleSep()
            ConsoleBtn("도착", C.Text) { e.arrive() }
            ConsoleBtn("초기화", C.Sub) { e.reset() }
        }
        Spacer(Modifier.width(12.dp))
        Box(
            Modifier.clip(CircleShape)
                .background(if (e.autoRunning) C.Coral else C.Mint)
                .tap { e.autoScenario() }
                .padding(horizontal = 22.dp, vertical = 12.dp),
        ) { T(if (e.autoRunning) "■ 멈춤" else "▶ 자동 시나리오", 19.sp, C.Bg, FontWeight.Bold) }
    }
}

@Composable
fun ConsoleBtn(label: String, color: Color, onClick: () -> Unit) {
    Box(
        Modifier.clip(CircleShape).background(color.copy(alpha = 0.1f)).border(1.dp, color.copy(alpha = 0.28f), CircleShape)
            .tap(onClick).padding(horizontal = 18.dp, vertical = 12.dp),
    ) { T(label, 18.sp, color, FontWeight.SemiBold) }
}

@Composable
fun ConsoleSep() = Box(Modifier.padding(horizontal = 4.dp).size(1.dp, 26.dp).background(C.Line))

// ───────────── 리포트 화면 ─────────────
@Composable
fun ReportScreen(e: RideEngine) {
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(22.dp)) {
        val careOnly = e.layout == Layout.CARE
        Glass(Modifier.width(400.dp).fillMaxHeight(), glow = if (careOnly) C.Amber else C.Mint) {
            if (careOnly) {
                SectionTitle("운전자에게 알린 일", e.careSummary())
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        T("${e.guardEvents}", 120.sp, C.Amber, FontWeight.Bold)
                        Spacer(Modifier.width(8.dp))
                        T("회", 32.sp, C.Sub, modifier = Modifier.padding(bottom = 26.dp))
                    }
                }
                T(
                    if (e.doorOpens > 0) "이동 중 문 열림이 있었어요" else if (e.guardEvents > 0) "알림 후 모두 안전하게 도착했어요" else "아무 일 없이 도착했어요",
                    24.sp, C.Text, FontWeight.Bold, Modifier.fillMaxWidth(), TextAlign.Center,
                )
            } else {
                SectionTitle("동승자 편안 점수", e.careSummary())
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    ScoreRing(e.comfort, Modifier.size(260.dp), 22.dp, 84.sp)
                }
                T(
                    when {
                        e.comfort >= 90 -> "아주 편안한 이동이었어요"
                        e.comfort >= 75 -> "대체로 편안했어요"
                        else -> "조금 힘든 이동이었어요"
                    },
                    24.sp, C.Text, FontWeight.Bold, Modifier.fillMaxWidth(), TextAlign.Center,
                )
            }
            Spacer(Modifier.height(6.dp))
            T("주행 ${fmtTime(e.tripSec)}", 17.sp, C.Sub, modifier = Modifier.fillMaxWidth(), align = TextAlign.Center)
        }
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(22.dp)) {
            Row(Modifier.fillMaxWidth().height(170.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                if (careOnly) {
                    Stat("문 열림", e.doorOpens, C.Coral, Modifier.weight(1f))
                    Stat("창문 열림", e.windowOpens, C.Amber, Modifier.weight(1f))
                    Stat("벨트 해제", e.beltOffs, C.Amber, Modifier.weight(1f))
                    Stat("자리 이동", e.seatMoves, C.Sky, Modifier.weight(1f))
                } else {
                    Stat("미리 알림", e.preAlerts, C.Sky, Modifier.weight(1f))
                    Stat("급제동", e.suddenBrakes, C.Coral, Modifier.weight(1f))
                    Stat("증상 알림", e.symptoms, C.Mint, Modifier.weight(1f))
                    if (e.layout == Layout.BOTH) Stat("돌봄 알림", e.guardEvents, C.Amber, Modifier.weight(1f))
                }
            }
            Glass(Modifier.fillMaxWidth().weight(1f)) {
                SectionTitle("다음엔 이렇게 해 보세요")
                Spacer(Modifier.height(14.dp))
                tips(e).forEach { (title, body, col) ->
                    Row(Modifier.padding(vertical = 9.dp), verticalAlignment = Alignment.Top) {
                        Box(Modifier.padding(top = 8.dp)) { Dot(col, 10.dp) }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            T(title, 20.sp, C.Text, FontWeight.SemiBold)
                            T(body, 16.sp, C.Sub)
                        }
                    }
                }
            }
        }
        Glass(Modifier.width(400.dp).fillMaxHeight()) {
            SectionTitle("이동 기록", "${e.log.size}건")
            Spacer(Modifier.height(10.dp))
            if (e.log.isEmpty()) {
                T("아직 기록이 없어요. 시연 콘솔의 ‘자동 시나리오’를 눌러 보세요.", 17.sp, C.Sub)
            }
            LazyColumn(Modifier.weight(1f)) {
                items(e.log.reversed()) { item ->
                    Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.Top) {
                        T(item.time.substring(0, 5), 14.sp, C.Dim, modifier = Modifier.width(52.dp).padding(top = 2.dp))
                        Box(Modifier.padding(top = 6.dp)) { Dot(levelColor(item.level), 8.dp) }
                        Spacer(Modifier.width(12.dp))
                        T(item.text, 16.sp, C.Text)
                    }
                }
            }
        }
    }
}

@Composable
fun Stat(label: String, value: Int, color: Color, modifier: Modifier) {
    Glass(modifier.fillMaxHeight(), padding = 22.dp, glow = color) {
        T(label, 17.sp, C.Sub)
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.Bottom) {
            T("$value", 56.sp, color, FontWeight.Bold)
            Spacer(Modifier.width(6.dp))
            T("회", 20.sp, C.Sub, modifier = Modifier.padding(bottom = 12.dp))
        }
    }
}

fun tips(e: RideEngine): List<Triple<String, String, Color>> {
    val out = mutableListOf<Triple<String, String, Color>>()
    if (e.suddenBrakes > 0 && e.layout != Layout.CARE) out += Triple("회생제동을 한 단계 낮춰 보세요", "급제동 ${e.suddenBrakes}회 · 전기차의 울컥거림이 멀미를 키워요", C.Coral)
    if (e.symptoms > 0) out += Triple("터널·긴 직선 구간 전에 미리 환기할게요", "증상 알림 ${e.symptoms}회 · 다음 주행부터 자동으로 반영", C.Mint)
    if (e.doorOpens > 0) out += Triple("출발 전 뒷좌석 차일드락을 꼭 확인해 주세요", "이동 중 문 열림 ${e.doorOpens}회 · 돌봄 동승자는 차일드락이 있는 뒷좌석이 안전해요", C.Coral)
    if (e.windowOpens > 0) out += Triple("출발 전에 운전석 창문 잠금 버튼을 켜 두세요", "창문 열림 ${e.windowOpens}회 · 출발할 때 라이드캄이 다시 알려 드릴게요", C.Amber)
    if (e.beltOffs > 0 || e.seatMoves > 0) out += Triple("다음엔 출발 전 벨트 확인을 한 번 더 할게요", "벨트 해제 ${e.beltOffs}회 · 자리 이동 ${e.seatMoves}회", C.Sky)
    if (e.layout != Layout.CARE) out += Triple("스무스 주행 모드를 켜 보세요", "가속·감속이 부드러워져 동승자가 더 편해요", C.Sky)
    return out.take(4)
}

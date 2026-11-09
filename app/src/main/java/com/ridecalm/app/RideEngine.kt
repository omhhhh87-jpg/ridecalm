package com.ridecalm.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.sign

enum class Seat(val label: String, val short: String, val rear: Boolean) {
    DRIVER("운전석", "운전석", false),
    PASSENGER("조수석", "조수석", false),
    REAR_L("뒷좌석 왼쪽", "뒤 왼쪽", true),
    REAR_R("뒷좌석 오른쪽", "뒤 오른쪽", true),
}

enum class CareMode(val label: String) { NONE("케어 없음"), MOTION("멀미 케어"), GUARD("돌봄") }

/** 선택한 케어 조합에 따라 화면 구성이 바뀐다. */
enum class Layout(val label: String) { MOTION("멀미"), CARE("돌봄"), BOTH("둘 다") }

/** 돌봄 대상. 진단명 대신 연령대로 고르고, 안내 음성의 말투가 달라진다. */
enum class CareTarget(val emoji: String, val label: String, val age: String, val who: String) {
    INFANT("👶", "영유아", "0~6세", "아이"),
    CHILD("🧒", "어린이", "7~12세", "아이"),
    YOUTH("🧑", "청소년·성인", "도움이 필요한", "동승자"),
    SENIOR("👵", "어르신", "65세 이상", "어르신"),
}

/** 출발 전 점검 한 줄 */
data class CheckItem(val label: String, val ok: Boolean, val fix: String)

enum class ManeuverKind { RIGHT, LEFT, BUMP, BRAKE, TUNNEL }

data class Maneuver(val kind: ManeuverKind, val title: String, val detail: String, val totalSec: Float)

enum class Level { INFO, CARE, DANGER }

data class Alert(
    val level: Level,
    val title: String,
    val detail: String,
    val actions: List<String> = emptyList(),
    /** 돌봄 알림의 위험도 표시("최우선", "즉시"). */
    val tier: String? = null,
    /** 운전자가 지금 할 일(한 줄) */
    val todo: String? = null,
    val id: Long = System.nanoTime(),
)

data class LogItem(val time: String, val text: String, val level: Level)

enum class Screen { CARE, REPORT }

/**
 * 라이드캄의 상태와 동작.
 * 차량 속도·기어는 Car API(VehicleBridge)에서 받고, 문·창문·안전벨트·착석 신호와
 * 차량 제어(공조·창문 잠금)·내비 예고는 시연용으로 흉내 낸다.
 * 안전 원칙: 돌봄에서 앱은 지켜보고 알리기만 한다. 문·창문은 앱이 스스로 잠그거나 열거나 닫지 않고,
 * 운전자가 직접 조작한다(앱의 '다시 잠그기'도 운전자가 눌렀을 때만 동작).
 */
class RideEngine(private val scope: CoroutineScope, private val speak: (String) -> Unit) {

    val seatModes = mutableStateMapOf(
        Seat.DRIVER to CareMode.NONE,
        Seat.PASSENGER to CareMode.MOTION,
        Seat.REAR_L to CareMode.NONE,
        Seat.REAR_R to CareMode.GUARD,
    )
    var sensitivity by mutableIntStateOf(2)
    var screen by mutableStateOf(Screen.CARE)
    var consoleOpen by mutableStateOf(true)

    // 차량 상태
    var speed by mutableFloatStateOf(0f)
    var gear by mutableStateOf("P")
    var vehicleLinked by mutableStateOf(false)
    private var targetSpeed = 0f
    private var simActive = false

    // 흔들림 예고
    var maneuver by mutableStateOf<Maneuver?>(null)
    var maneuverLeft by mutableFloatStateOf(0f)
    var nextHint by mutableStateOf("출발하면 회전·제동을 미리 알려 드려요")

    // 실내 환경 (제어 SDK 공개 전까지 시뮬레이션)
    var freshAir by mutableStateOf(false)
    var purifier by mutableStateOf(false)
    var tempC by mutableFloatStateOf(24f)
    var fan by mutableIntStateOf(2)
    var seatVent by mutableIntStateOf(0)
    var ambient by mutableStateOf("기본")
    var ventBoost by mutableStateOf(false)

    // 돌봄
    var careTarget by mutableStateOf(CareTarget.CHILD)

    /** 동승자에게 들려주는 안내 음성. 말을 이해하기 어려운 동승자(영유아, 발달 연령이 낮은 아이)는 끈다. */
    var passengerVoice by mutableStateOf(true)

    // 가까운 휴게소(실차에서는 NaviHelper 경로 정보로 계산). 주행하면 줄어든다.
    var restKm by mutableFloatStateOf(2.4f)
    var doorLock by mutableStateOf(false)
    var childLockAsked by mutableStateOf(false)
    var windowLock by mutableStateOf(false)
    val windowOpen = mutableStateMapOf<Seat, Int>()
    val belt = mutableStateMapOf<Seat, Boolean>()
    val doorOpen = mutableStateMapOf<Seat, Boolean>()
    val occupied = mutableStateMapOf<Seat, Boolean>()

    /** 출발부터 목적지 도착까지. 신호 대기로 멈춰 있어도 이동 중으로 본다. */
    var inTrip by mutableStateOf(false)
    private var arrived = false

    /** 에어백 전개 등 사고 신호를 받아 앱 동작을 멈춘 상태 */
    var safetyStop by mutableStateOf(false)
    private var startWarnedAt = 0L
    var doorOpens by mutableIntStateOf(0)
    var windowOpens by mutableIntStateOf(0)
    var beltOffs by mutableIntStateOf(0)
    var seatMoves by mutableIntStateOf(0)
    private val lastAlertAt = mutableMapOf<String, Long>()

    // 알림·기록
    var alert by mutableStateOf<Alert?>(null)
    val log = mutableStateListOf<LogItem>()
    var comfort by mutableIntStateOf(100)
    var tripSec by mutableIntStateOf(0)
    var preAlerts by mutableIntStateOf(0)
    var suddenBrakes by mutableIntStateOf(0)
    var symptoms by mutableIntStateOf(0)
    var guardEvents by mutableIntStateOf(0)
    var autoRunning by mutableStateOf(false)

    private var alertJob: Job? = null
    private var autoJob: Job? = null
    private val timeFmt = DateTimeFormatter.ofPattern("HH:mm:ss")

    val moving get() = speed > 0.5f
    val motionSeats get() = Seat.entries.filter { seatModes[it] == CareMode.MOTION }
    val guardSeats get() = Seat.entries.filter { seatModes[it] == CareMode.GUARD }
    val layout: Layout
        get() = when {
            guardSeats.isEmpty() && motionSeats.isNotEmpty() -> Layout.MOTION
            motionSeats.isEmpty() && guardSeats.isNotEmpty() -> Layout.CARE
            else -> Layout.BOTH
        }

    init {
        resetCabin()
        scope.launch {
            var ticks = 0
            while (isActive) {
                delay(100)
                val d = targetSpeed - speed
                val step = if (d < 0) 3.5f else 1.4f
                speed = if (abs(d) <= step) targetSpeed else speed + sign(d) * step
                if (maneuver != null) {
                    maneuverLeft = (maneuverLeft - 0.1f).coerceAtLeast(0f)
                    if (maneuverLeft <= 0f) {
                        maneuver = null
                        ambient = if (moving) "잔잔하게" else "기본"
                    }
                }
                if (speed > 0.5f) {
                    restKm -= speed / 36000f
                    if (restKm < 0.2f) restKm = 8.6f // 지나치면 다음 휴게소
                }
                if (++ticks >= 10) {
                    ticks = 0
                    if (moving) tripSec++
                }
            }
        }
    }

    // ---- 차량에서 들어오는 값 ----
    fun onVehicleSpeed(kmh: Float) {
        if (simActive) return
        speed = kmh; targetSpeed = kmh
    }

    fun onVehicleGear(g: String) {
        if (!simActive) gear = g
    }

    // ---- 설정 ----
    fun cycleSeat(s: Seat) {
        if (moving) {
            show(Alert(Level.INFO, "정차 중에만 바꿀 수 있어요", "주행 중에는 케어 설정을 바꾸지 않아요."))
            return
        }
        if (s == Seat.DRIVER) {
            show(Alert(Level.INFO, "운전석은 운전 팁으로 도와드려요", "동승자 좌석을 눌러 케어를 정해 주세요."))
            return
        }
        val next = when (seatModes[s]) {
            CareMode.NONE -> CareMode.MOTION
            CareMode.MOTION -> CareMode.GUARD
            else -> CareMode.NONE
        }
        seatModes[s] = next
        addLog("${s.label} → ${next.label}", Level.INFO)
    }

    /** 멀미만 / 돌봄만 / 둘 다 — 대표 좌석 배치로 한 번에 바꾼다. */
    fun selectLayout(l: Layout) {
        if (moving) {
            show(Alert(Level.INFO, "정차 중에만 바꿀 수 있어요", "주행 중에는 케어 설정을 바꾸지 않아요."))
            return
        }
        if (l == layout && (motionSeats.isNotEmpty() || guardSeats.isNotEmpty())) return
        Seat.entries.forEach { seatModes[it] = CareMode.NONE }
        when (l) {
            Layout.MOTION -> seatModes[Seat.PASSENGER] = CareMode.MOTION
            Layout.CARE -> seatModes[Seat.REAR_R] = CareMode.GUARD
            Layout.BOTH -> { seatModes[Seat.PASSENGER] = CareMode.MOTION; seatModes[Seat.REAR_R] = CareMode.GUARD }
        }
        addLog("케어 선택 → ${l.label}", Level.INFO)
    }

    fun selectTarget(t: CareTarget) {
        if (moving) {
            show(Alert(Level.INFO, "정차 중에만 바꿀 수 있어요", "주행 중에는 돌봄 설정을 바꾸지 않아요."))
            return
        }
        careTarget = t
        passengerVoice = t != CareTarget.INFANT
        addLog("돌봄 대상 → ${t.label}(${t.age})", Level.INFO)
    }

    /** 돌봄 좌석의 출발 전 점검: 문 · 창문 · 안전벨트 · 착석 */
    fun preCheck(): List<CheckItem> {
        val s = guardSeats.firstOrNull() ?: Seat.REAR_R
        return listOf(
            CheckItem("문 닫힘", doorOpen[s] != true, "문을 닫아 주세요"),
            CheckItem("창문 닫힘", (windowOpen[s] ?: 0) == 0, "창문을 닫아 주세요"),
            CheckItem("안전벨트 착용", belt[s] != false, "안전벨트를 매 주세요"),
            CheckItem("자리에 앉음", occupied[s] != false, "자리에 앉도록 도와주세요"),
        )
    }

    /** 돌봄 대상에게 들려줄 말. 영유아는 말 대신 운전자에게만 알린다. */
    private fun passengerLine(simple: String, polite: String): String? = when {
        !passengerVoice -> null
        careTarget == CareTarget.SENIOR -> polite
        else -> simple
    }

    private fun passengerNote(line: String?): String =
        if (line == null) "동승자 안내 음성을 꺼 두어 운전자에게만 알렸어요." else "${careTarget.who}에게 \"$line\" 안내 음성을 들려줬어요."

    private fun passengerActions(vararg first: String): List<String> =
        first.toList() + if (!passengerVoice) emptyList() else listOf("${careTarget.who} 안내 음성")

    fun togglePassengerVoice() {
        passengerVoice = !passengerVoice
        addLog("동승자 안내 음성 ${if (passengerVoice) "켬" else "끔"}", Level.INFO)
    }

    /** 가까운 휴게소까지 남은 거리·시간 */
    fun restText(): String = "가까운 휴게소 " + restShort()

    fun restShort(): String {
        val min = kotlin.math.ceil(restKm / maxOf(speed, 40f) * 60f).toInt().coerceAtLeast(1)
        return "%.1fkm · 약 ${min}분".format(restKm)
    }

    /** 운전자가 확인하면 휴게소를 내비 경유지로 추가(NaviHelper) */
    fun addRestStop() {
        show(Alert(Level.CARE, "휴게소를 경유지로 추가했어요", "${restText()} · 내비가 안내를 시작해요.", listOf("내비 경유지 추가")))
        speak("가까운 휴게소를 경유지로 추가했어요.")
        addLog("휴게소 경유지 추가 · ${restText()}", Level.CARE)
    }

    // ---- 주행 이벤트 ----
    fun startDrive() {
        if (simActive && moving) return
        // 출발 전 점검: 하나라도 안 되면 먼저 알린다(앱은 출발을 막지 않으므로, 한 번 더 누르면 그대로 출발).
        if (guardSeats.isNotEmpty() && !simActive) {
            val checks = preCheck()
            val bad = checks.filter { !it.ok }
            val now = System.currentTimeMillis()
            if (bad.isNotEmpty() && now - startWarnedAt > 15_000) {
                startWarnedAt = now
                val seat = guardSeats.first()
                show(
                    Alert(
                        Level.DANGER, "출발 전에 확인해 주세요",
                        "${careTarget.emoji} ${seat.label} · " + checks.joinToString("   ") { (if (it.ok) "✓ " else "✕ ") + it.label },
                        tier = "출발 전",
                        todo = bad.joinToString(" · ") { it.fix },
                    ),
                )
                speak("출발 전에 확인해 주세요. ${bad.joinToString(", ") { it.fix }}.")
                addLog("출발 전 점검 ${4 - bad.size}/4 → ${bad.joinToString { it.label }} 확인 필요", Level.CARE)
                return
            }
        }
        simActive = true; inTrip = true; safetyStop = false; arrived = false
        gear = "D"; targetSpeed = 38f
        doorLock = true // 차량이 주행 시 자동으로 잠그는 상태를 확인만 한다
        val actions = mutableListOf("문 잠금 상태 확인")
        var voice = "출발할게요. 편안하게 모실게요."
        if (guardSeats.isNotEmpty()) {
            childLockAsked = true
            val ok = preCheck().count { it.ok }
            actions += listOf("출발 전 점검 $ok/4", "창문 잠금 · 차일드락 확인 안내")
            voice = "출발할게요. 운전석 창문 잠금 버튼과 뒷좌석 차일드락을 확인해 주세요."
        }
        if (motionSeats.isNotEmpty()) {
            freshAir = true; purifier = true; ventBoost = true; fan = 3
            actions += "탑승 직후 환기"
            scope.launch { delay(8000); ventBoost = false }
        }
        ambient = "잔잔하게"
        nextHint = "300m 앞 우회전"
        show(Alert(Level.INFO, "출발할게요", careSummary(), actions))
        speak(voice)
        addLog("출발 · ${actions.joinToString(", ")}", Level.INFO)
    }

    fun turn(right: Boolean) {
        ensureMoving()
        val dir = if (right) "오른쪽" else "왼쪽"
        preview(
            Maneuver(if (right) ManeuverKind.RIGHT else ManeuverKind.LEFT, "곧 ${dir}으로 돌아요", "300m 앞 ${if (right) "우회전" else "좌회전"} · 몸을 등받이에 기대 주세요", 6f),
            "곧 ${dir}으로 돌아요",
        )
        ambient = "${dir}부터 밝게"
        nextHint = if (right) "1.2km 앞 과속방지턱" else "목적지까지 800m"
        scope.launch { delay(6000); targetSpeed = 18f; delay(2200); if (simActive) targetSpeed = 38f }
    }

    fun bump() {
        ensureMoving()
        preview(Maneuver(ManeuverKind.BUMP, "과속방지턱이 있어요", "150m 앞 · 속도를 미리 줄여요", 4.5f), "앞에 과속방지턱이 있어요")
        nextHint = "2km 앞 터널"
        scope.launch { delay(2500); targetSpeed = 20f; delay(3000); if (simActive) targetSpeed = 38f }
    }

    fun tunnel() {
        ensureMoving()
        freshAir = false; purifier = true
        preview(Maneuver(ManeuverKind.TUNNEL, "곧 터널에 들어가요", "바깥 공기를 잠시 막고 공기청정을 켰어요", 5f), "곧 터널이에요. 바깥 공기를 잠시 막을게요")
        nextHint = "500m 앞 좌회전"
        scope.launch { delay(9000); if (motionSeats.isNotEmpty()) freshAir = true }
    }

    fun brake() {
        ensureMoving()
        suddenBrakes++
        comfort = (comfort - 4).coerceAtLeast(0)
        speed = (speed - 14f).coerceAtLeast(6f); targetSpeed = 10f
        show(Alert(Level.CARE, "급제동이 있었어요", "놀라셨죠? 천천히 숨을 쉬어요.", listOf("운전 리포트에 기록")))
        addLog("급제동", Level.CARE)
        scope.launch { delay(2500); if (simActive) targetSpeed = 38f }
    }

    fun sick() {
        val s = motionSeats.firstOrNull() ?: Seat.PASSENGER
        symptoms++
        comfort = (comfort - 6).coerceAtLeast(0)
        tempC = 21f; fan = 4; seatVent = 2; freshAir = true
        show(
            Alert(
                Level.CARE, "${s.label} 동승자가 속이 안 좋아요",
                "시원한 바깥 공기를 보냈어요. ${restText()}이에요.",
                listOf("찬바람 강 · 바깥 공기", "통풍시트 2단", "휴게소 제안"),
                todo = "\"응\"이라고 말하면 휴게소를 경유지로 추가해요",
            ),
        )
        speak("시원한 바람을 보낼게요. 천천히 숨 쉬세요.")
        addLog("${s.label} 증상 알림 → 찬바람·바깥 공기·통풍시트", Level.CARE)
    }

    fun smell() {
        ventBoost = true; freshAir = true; fan = 5; purifier = true
        show(Alert(Level.CARE, "실내 냄새를 빼고 있어요", "20초 동안 강하게 환기할게요.", listOf("바깥 공기", "풍량 최대 20초", "공기청정")))
        speak("냄새를 빼기 위해 잠깐 강하게 환기할게요.")
        addLog("냄새 케어 · 강한 환기 20초", Level.CARE)
        scope.launch { delay(20000); ventBoost = false; fan = 3 }
    }

    // ---- 돌봄 신호 (실차에서는 Door·Window·Seat·Safety 도메인의 register 콜백으로 들어온다) ----

    /** 돌봄 좌석 창문이 열림 → 운전자에게 알리고, 닫기·잠금은 운전자가 직접 한다. */
    fun rearWindow() {
        if (safetyStop) return
        val s = guardSeats.firstOrNull() ?: Seat.REAR_R
        windowLock = false
        windowOpen[s] = 40
        if (!inTrip) { addLog("${s.label} 창문 열림(출발 전)", Level.CARE); return }
        windowOpens++
        if (!firstInWindow("window")) { addLog("${s.label} 창문 열림 (반복 알림 생략)", Level.CARE); return }
        guardEvents++
        show(
            Alert(
                Level.DANGER, "${s.label} 창문이 열렸어요",
                "40% 열렸어요. " + passengerNote(passengerLine("손을 안으로 넣어요", "손을 안으로 넣어 주세요")),
                passengerActions("운전자 음성 알림"),
                tier = "즉시",
                todo = "운전석 스위치로 닫고 창문 잠금 버튼을 켜 주세요",
            ),
        )
        speak("${s.label} 창문이 열렸어요. 운전석 스위치로 닫고 창문 잠금 버튼을 켜 주세요.")
        addLog("${s.label} 창문 열림 40% → 운전자 알림", Level.DANGER)
    }

    /** 운전자가 운전석 스위치로 직접 닫고 창문 잠금 버튼을 켜는 동작(시연용, 차량에서 들어오는 신호) */
    fun driverClosesWindow() {
        val s = Seat.entries.firstOrNull { (windowOpen[it] ?: 0) > 0 } ?: return
        scope.launch {
            while ((windowOpen[s] ?: 0) > 0) { delay(100); windowOpen[s] = ((windowOpen[s] ?: 0) - 3).coerceAtLeast(0) }
            windowLock = true
            addLog("운전자가 ${s.label} 창문을 닫고 창문 잠금을 켬", Level.INFO)
        }
    }

    /** 문 열림 감지. 도착 전(신호 대기 포함)이면 최우선 경고, 도착 후면 하차 안내. */
    fun doorOpened() {
        if (safetyStop) return
        val s = guardSeats.firstOrNull() ?: Seat.REAR_R
        doorOpen[s] = true
        scope.launch { delay(4500); doorOpen[s] = false; addLog("${s.label} 문 닫힘", Level.INFO) }
        if (!inTrip && !arrived) { addLog("${s.label} 문 열림(출발 전)", Level.INFO); return }
        if (inTrip) {
            guardEvents++; doorOpens++
            ambient = "경고 표시"
            show(
                Alert(
                    Level.DANGER, "${s.label} 문이 열렸어요",
                    (if (moving) "" else "아직 목적지 도착 전이에요. ") + passengerNote(passengerLine("자리에 가만히 앉아 있어요. 곧 멈출게요", "자리에 앉아 계세요. 곧 멈출게요")),
                    passengerActions("운전자 음성 알림", "경고 조명"),
                    tier = "최우선",
                    todo = if (moving) "천천히 멈추고 비상등을 켜 주세요" else "출발하기 전에 문이 닫혔는지 확인해 주세요",
                ),
            )
            speak("${s.label} 문이 열렸어요. 천천히 멈추고 비상등을 켜 주세요.")
            addLog("${s.label} 이동 중 문 열림 → 최우선 경고", Level.DANGER)
            scope.launch { delay(5000); if (ambient == "경고 표시") ambient = if (moving) "잔잔하게" else "기본" }
        } else {
            show(Alert(Level.INFO, "${s.label} 문이 열렸어요", "도착했어요. 뒤에서 오는 차를 먼저 확인해 주세요.", listOf("후방 확인 안내")))
            addLog("${s.label} 도착 후 문 열림 → 후방 확인 안내", Level.INFO)
        }
    }

    fun beltOff() {
        if (safetyStop) return
        val s = guardSeats.firstOrNull() ?: Seat.PASSENGER
        if (!inTrip) { // 출발 전에는 점검표만 바뀐다(누를 때마다 풀림/착용)
            belt[s] = belt[s] == false
            addLog("${s.label} 안전벨트 ${if (belt[s] == true) "착용" else "풀림"}(출발 전)", Level.INFO)
            return
        }
        belt[s] = false
        beltOffs++
        scope.launch { delay(7000); belt[s] = true; addLog("${s.label} 안전벨트 다시 착용", Level.INFO) }
        if (!firstInWindow("belt")) { addLog("${s.label} 안전벨트 풀림 (반복 알림 생략)", Level.CARE); return }
        guardEvents++
        show(
            Alert(
                Level.DANGER, "${s.label} 안전벨트가 풀렸어요",
                passengerNote(passengerLine("자리에 앉아 있어요. 곧 멈출게요", "자리에 앉아 계세요. 곧 멈출게요")) + " 멈출 곳: ${restText()}",
                passengerActions("운전자 음성 알림", "멈출 곳 안내"),
                tier = "즉시",
                todo = "안전한 곳에 멈춰 벨트를 다시 매 주세요",
            ),
        )
        speak("${s.label} 안전벨트가 풀렸어요. ${restText()}이에요.")
        addLog("${s.label} 안전벨트 풀림 → 운전자 알림", Level.DANGER)
    }

    /** 착석 신호: 돌봄 좌석이 비고 다른 좌석에 사람이 앉음(착석 신호를 지원하는 차량) */
    fun seatMove() {
        if (safetyStop) return
        val s = guardSeats.firstOrNull() ?: Seat.REAR_R
        val to = if (s == Seat.REAR_R) Seat.REAR_L else Seat.REAR_R
        occupied[s] = false; occupied[to] = true
        if (!inTrip) {
            scope.launch { delay(5000); occupied[s] = true; occupied[to] = false }
            addLog("${s.label} 자리 비움(출발 전)", Level.INFO); return
        }
        seatMoves++
        scope.launch { delay(7000); occupied[s] = true; occupied[to] = false; addLog("${s.label}에 다시 앉음", Level.INFO) }
        if (!firstInWindow("seat")) { addLog("${s.label} 자리 이동 (반복 알림 생략)", Level.CARE); return }
        guardEvents++
        show(
            Alert(
                Level.DANGER, "${s.label} 자리가 비었어요",
                "${to.label}으로 옮겨 앉은 것 같아요. " + passengerNote(passengerLine("자리에 앉아요", "자리에 앉아 계세요")) + " 멈출 곳: ${restText()}",
                passengerActions("운전자 음성 알림", "멈출 곳 안내"),
                tier = "즉시",
                todo = "안전한 곳에 멈춰 자리를 확인해 주세요",
            ),
        )
        speak("${s.label} 자리가 비었어요. ${restText()}이에요.")
        addLog("${s.label} 자리 이동 → 운전자 알림", Level.DANGER)
    }

    /** 에어백 전개(Safety) → 앱의 제어·알림을 즉시 멈추고 차량 기본 안전 기능에 맡긴다. */
    fun airbagDeployed() {
        autoJob?.cancel(); autoRunning = false
        safetyStop = true
        maneuver = null; ambient = "기본"
        show(Alert(Level.DANGER, "사고 신호를 받아 라이드캄을 멈췄어요", "차량 기본 안전 기능이 우선합니다. 앱의 잠금·조명·알림을 모두 멈췄어요.", listOf("앱 제어 중단", "차량 기본 기능 우선")))
        addLog("에어백 전개 신호 → 라이드캄 동작 중단", Level.DANGER)
    }

    // 운전자가 돌봄 동승자에게 손을 쓰지 않고 해 줄 수 있는 것
    fun reassure() {
        val s = guardSeats.firstOrNull() ?: Seat.REAR_R
        val line = when (careTarget) {
            CareTarget.SENIOR -> "조금만 기다려 주세요. 곧 도착해요."
            CareTarget.INFANT -> "잔잔한 자장가를 틀었어요."
            else -> "조금만 기다려요. 곧 도착해요."
        }
        show(Alert(Level.INFO, "${s.label}에 안심 음성을 들려줬어요", "“$line”", listOf("차 스피커", "무드조명 잔잔하게")))
        speak(line)
        ambient = "잔잔하게"
        addLog("${s.label} 안심 음성", Level.INFO)
    }

    /** 운전자가 버튼을 눌렀을 때만 잠금을 다시 켠다. 열린 창문을 닫는 것은 운전자가 직접 한다. */
    fun relock() {
        if (safetyStop) return
        doorLock = true; windowLock = true
        val open = Seat.entries.any { (windowOpen[it] ?: 0) > 0 }
        show(
            Alert(
                Level.CARE, "잠금을 다시 켰어요",
                if (open) "문 잠금 · 창문 잠금을 켰어요." else "문 잠금 · 돌봄 좌석 창문 잠금을 켰어요.",
                listOf("문 잠금", "창문 잠금"),
                todo = if (open) "열린 창문은 운전석 스위치로 닫아 주세요" else null,
            ),
        )
        speak("잠금을 다시 켰어요.")
        addLog("문·창문 잠금 다시 켬", Level.CARE)
    }

    /** 같은 종류의 돌봄 알림은 20초 안에 한 번만 띄워 운전자 주의를 흩뜨리지 않는다. */
    private fun firstInWindow(kind: String): Boolean {
        val now = System.currentTimeMillis()
        val last = lastAlertAt[kind]
        if (last != null && now - last < 20_000) return false
        lastAlertAt[kind] = now
        return true
    }

    fun arrive() {
        targetSpeed = 0f
        nextHint = "목적지 도착"
        scope.launch {
            while (speed > 0.5f) delay(100)
            gear = "P"
            simActive = false; inTrip = false; arrived = true
            maneuver = null
            ambient = "기본"; ventBoost = false; fan = 2; tempC = 24f; seatVent = 0
            val hasRear = guardSeats.isNotEmpty()
            show(
                Alert(
                    Level.INFO, "도착했어요",
                    if (hasRear) "이번 주행: ${tripSummary()} · 뒤에서 오는 차를 확인한 뒤 인도 쪽 문으로 내려요." else "편안한 이동이었기를 바라요.",
                    if (hasRear) listOf("이번 주행 요약", "후방 확인 안내", "인도 쪽 하차 안내") else listOf("편안 리포트 만들기"),
                ),
            )
            speak("도착했어요. 뒤에서 오는 차를 확인한 뒤 내려 주세요.")
            addLog("도착 · 안전한 하차 안내", Level.INFO)
            delay(3500)
            doorLock = false
            screen = Screen.REPORT
        }
    }

    fun autoScenario() {
        if (autoRunning) {
            autoJob?.cancel(); autoRunning = false
            return
        }
        reset()
        autoRunning = true
        autoJob = scope.launch {
            if (layout == Layout.CARE) {
                // 출발 전: 막 타서 벨트를 안 매고 창문이 조금 열린 상태
                val s = guardSeats.firstOrNull() ?: Seat.REAR_R
                belt[s] = false; windowOpen[s] = 15
                delay(3000)
                startDrive(); delay(5500) // 점검 2/4 → 알림
                belt[s] = true; addLog("${s.label} 안전벨트 착용", Level.INFO)
                driverClosesWindow(); delay(3000)
            }
            startDrive(); delay(4500)
            when (layout) {
                Layout.MOTION -> {
                    turn(true); delay(8000)
                    bump(); delay(6000)
                    tunnel(); delay(6500)
                    sick(); delay(4500)
                    addRestStop(); delay(4000)
                    brake(); delay(5000)
                    turn(false); delay(8000)
                    smell(); delay(5500)
                }
                Layout.CARE -> {
                    delay(2500)
                    rearWindow(); delay(4500)
                    driverClosesWindow(); delay(4000)
                    beltOff(); delay(7500)
                    reassure(); delay(5500)
                    seatMove(); delay(7500)
                    targetSpeed = 0f; delay(3500) // 신호 대기
                    doorOpened(); delay(7000)
                    targetSpeed = 38f; delay(3000)
                }
                Layout.BOTH -> {
                    turn(true); delay(8000)
                    bump(); delay(6000)
                    tunnel(); delay(6500)
                    rearWindow(); delay(4500)
                    driverClosesWindow(); delay(3500)
                    sick(); delay(7500)
                    doorOpened(); delay(7000)
                    brake(); delay(5000)
                    turn(false); delay(8000)
                    smell(); delay(5500)
                }
            }
            arrive(); delay(4500)
            autoRunning = false
        }
    }

    fun reset() {
        autoJob?.cancel(); autoRunning = false
        simActive = false; targetSpeed = 0f; speed = 0f; gear = "P"
        maneuver = null; alert = null
        comfort = 100; tripSec = 0; preAlerts = 0; suddenBrakes = 0; symptoms = 0; guardEvents = 0
        doorOpens = 0; windowOpens = 0; beltOffs = 0; seatMoves = 0
        freshAir = false; purifier = false; tempC = 24f; fan = 2; seatVent = 0; ambient = "기본"; ventBoost = false
        doorLock = false; windowLock = false; childLockAsked = false
        inTrip = false; safetyStop = false; arrived = false; startWarnedAt = 0L
        lastAlertAt.clear()
        restKm = 2.4f
        resetCabin()
        nextHint = "출발하면 회전·제동을 미리 알려 드려요"
        log.clear()
        screen = Screen.CARE
    }

    fun dismissAlert() { alert = null }

    fun careSummary(): String {
        val parts = mutableListOf<String>()
        if (motionSeats.isNotEmpty()) parts += "멀미 케어 ${motionSeats.joinToString { it.label }}"
        if (guardSeats.isNotEmpty()) parts += "돌봄 ${guardSeats.joinToString { it.label }}"
        return if (parts.isEmpty()) "케어할 좌석이 없어요" else parts.joinToString(" · ")
    }

    fun tripSummary(): String =
        "문 열림 ${doorOpens}회 · 창문 열림 ${windowOpens}회 · 벨트 해제 ${beltOffs}회" + if (seatMoves > 0) " · 자리 이동 ${seatMoves}회" else ""

    private fun resetCabin() {
        Seat.entries.forEach { windowOpen[it] = 0; belt[it] = true; doorOpen[it] = false; occupied[it] = seatModes[it] != CareMode.NONE || it == Seat.DRIVER }
    }

    private fun ensureMoving() {
        if (!moving && !simActive) startDrive()
    }

    private fun preview(m: Maneuver, voice: String) {
        maneuver = m; maneuverLeft = m.totalSec
        preAlerts++
        speak(voice)
        addLog("미리 알림 · ${m.title}", Level.INFO)
    }

    private fun show(a: Alert) {
        if (safetyStop && a.level != Level.DANGER) return
        alert = a
        alertJob?.cancel()
        alertJob = scope.launch {
            delay(if (a.level == Level.DANGER) 7000 else 5500)
            if (alert?.id == a.id) alert = null
        }
    }

    private fun addLog(text: String, level: Level) {
        log.add(0, LogItem(LocalTime.now().format(timeFmt), text, level))
    }
}

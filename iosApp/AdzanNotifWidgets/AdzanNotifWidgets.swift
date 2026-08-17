import SwiftUI
import WidgetKit

private let appGroupIdentifier = "group.com.adzannotif.app"

private struct PrayerSnapshot: Codable {
    let nextPrayerId: String?
    let targetEpochMillis: Int64?
    let locationName: String?
    let availability: String
}

private struct CelestialSnapshot: Codable {
    let kind: String
    let phaseName: String?
    let illuminationPercent: Double?
    let nextEventTitle: String?
    let nextEventEpochMillis: Int64?
    let availability: String
}

private enum SnapshotStore {
    static func load<T: Decodable>(_ type: T.Type, key: String) -> T? {
        guard
            let raw = UserDefaults(suiteName: appGroupIdentifier)?.string(forKey: key),
            let data = raw.data(using: .utf8)
        else { return nil }
        return try? JSONDecoder().decode(type, from: data)
    }

    static func date(_ epochMillis: Int64?) -> Date? {
        guard let epochMillis else { return nil }
        return Date(timeIntervalSince1970: TimeInterval(epochMillis) / 1_000.0)
    }
}

private struct PrayerEntry: TimelineEntry {
    let date: Date
    let snapshot: PrayerSnapshot?
}

private struct MoonEntry: TimelineEntry {
    let date: Date
    let snapshot: CelestialSnapshot?
}

private struct SunEntry: TimelineEntry {
    let date: Date
    let snapshot: CelestialSnapshot?
}

private struct PrayerProvider: TimelineProvider {
    func placeholder(in context: Context) -> PrayerEntry {
        PrayerEntry(date: Date(), snapshot: nil)
    }

    func getSnapshot(in context: Context, completion: @escaping (PrayerEntry) -> Void) {
        completion(PrayerEntry(
            date: Date(),
            snapshot: SnapshotStore.load(PrayerSnapshot.self, key: "prayer_snapshot")
        ))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<PrayerEntry>) -> Void) {
        let snapshot = SnapshotStore.load(PrayerSnapshot.self, key: "prayer_snapshot")
        completion(Timeline(entries: [PrayerEntry(date: Date(), snapshot: snapshot)], policy: .after(nextRefresh(snapshot?.targetEpochMillis))))
    }
}

private struct MoonProvider: TimelineProvider {
    func placeholder(in context: Context) -> MoonEntry {
        MoonEntry(date: Date(), snapshot: nil)
    }

    func getSnapshot(in context: Context, completion: @escaping (MoonEntry) -> Void) {
        completion(MoonEntry(
            date: Date(),
            snapshot: SnapshotStore.load(CelestialSnapshot.self, key: "moon_snapshot")
        ))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<MoonEntry>) -> Void) {
        let snapshot = SnapshotStore.load(CelestialSnapshot.self, key: "moon_snapshot")
        completion(Timeline(entries: [MoonEntry(date: Date(), snapshot: snapshot)], policy: .after(nextRefresh(snapshot?.nextEventEpochMillis))))
    }
}

private struct SunProvider: TimelineProvider {
    func placeholder(in context: Context) -> SunEntry {
        SunEntry(date: Date(), snapshot: nil)
    }

    func getSnapshot(in context: Context, completion: @escaping (SunEntry) -> Void) {
        completion(SunEntry(
            date: Date(),
            snapshot: SnapshotStore.load(CelestialSnapshot.self, key: "sun_snapshot")
        ))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<SunEntry>) -> Void) {
        let snapshot = SnapshotStore.load(CelestialSnapshot.self, key: "sun_snapshot")
        completion(Timeline(entries: [SunEntry(date: Date(), snapshot: snapshot)], policy: .after(nextRefresh(snapshot?.nextEventEpochMillis))))
    }
}

private func nextRefresh(_ epochMillis: Int64?) -> Date {
    let now = Date()
    guard let target = SnapshotStore.date(epochMillis), target > now else {
        return now.addingTimeInterval(3_600)
    }
    return target.addingTimeInterval(1)
}

private struct PrayerWidgetView: View {
    let entry: PrayerEntry

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            if let snapshot = entry.snapshot, snapshot.availability == "AVAILABLE" {
                Text(snapshot.nextPrayerId ?? "Data tidak tersedia")
                    .font(.headline)
                Text(snapshot.locationName ?? "Lokasi belum tersedia")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                if let target = SnapshotStore.date(snapshot.targetEpochMillis), target > entry.date {
                    Text(target, style: .timer)
                        .font(.title3.monospacedDigit())
                } else {
                    Text("Waktu belum tersedia")
                        .font(.caption)
                }
            } else {
                Text("Jadwal sholat belum tersedia")
                    .font(.headline)
                Text("Buka aplikasi untuk memilih lokasi.")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
        .containerBackground(.background, for: .widget)
    }
}

private struct MoonWidgetView: View {
    let entry: MoonEntry

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            if let snapshot = entry.snapshot, snapshot.availability == "AVAILABLE" {
                Text(snapshot.phaseName ?? "Fase bulan belum tersedia")
                    .font(.headline)
                if let illumination = snapshot.illuminationPercent {
                    Text(String(format: "%.1f%% iluminasi", illumination))
                        .font(.caption)
                }
                eventContent(title: snapshot.nextEventTitle, epochMillis: snapshot.nextEventEpochMillis)
            } else {
                Text("Data bulan belum tersedia")
                    .font(.headline)
                Text("Buka aplikasi untuk memilih lokasi.")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
        .containerBackground(Color(red: 0.07, green: 0.11, blue: 0.19), for: .widget)
    }
}

private struct SunWidgetView: View {
    let entry: SunEntry

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            if let snapshot = entry.snapshot, snapshot.availability == "AVAILABLE" {
                Text(snapshot.phaseName ?? "Fase surya belum tersedia")
                    .font(.headline)
                eventContent(title: snapshot.nextEventTitle, epochMillis: snapshot.nextEventEpochMillis)
            } else {
                Text("Data surya belum tersedia")
                    .font(.headline)
                Text("Buka aplikasi untuk memilih lokasi.")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
        .containerBackground(Color(red: 0.04, green: 0.08, blue: 0.14), for: .widget)
    }
}

private func eventContent(title: String?, epochMillis: Int64?) -> some View {
    VStack(alignment: .leading, spacing: 2) {
        Text(title ?? "Peristiwa berikutnya belum tersedia")
            .font(.caption)
        if let target = SnapshotStore.date(epochMillis), target > Date() {
            Text(target, style: .timer)
                .font(.title3.monospacedDigit())
        } else {
            Text("Waktu belum tersedia")
                .font(.caption)
                .foregroundStyle(.secondary)
        }
    }
}

struct PrayerWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "prayer_widget", provider: PrayerProvider()) { entry in
            PrayerWidgetView(entry: entry)
        }
        .configurationDisplayName("Jadwal sholat")
        .description("Waktu sholat berikutnya dari snapshot offline aplikasi.")
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}

struct MoonWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "moon_widget", provider: MoonProvider()) { entry in
            MoonWidgetView(entry: entry)
        }
        .configurationDisplayName("Bulan")
        .description("Fase dan peristiwa bulan dari snapshot offline aplikasi.")
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}

struct SunWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "sun_widget", provider: SunProvider()) { entry in
            SunWidgetView(entry: entry)
        }
        .configurationDisplayName("Matahari")
        .description("Fase dan peristiwa surya dari snapshot offline aplikasi.")
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}

@main
struct AdzanNotifWidgets: WidgetBundle {
    var body: some Widget {
        PrayerWidget()
        MoonWidget()
        SunWidget()
    }
}

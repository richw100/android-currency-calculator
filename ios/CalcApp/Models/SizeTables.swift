import Foundation

struct SizeCountry {
    let key: String
    let displayName: String
    let unit: String

    init(_ key: String, _ displayName: String, unit: String = "") {
        self.key = key
        self.displayName = displayName
        self.unit = unit
    }
}

let shoeCountries: [SizeCountry] = [
    SizeCountry("EU", "EU"), SizeCountry("UK", "UK"), SizeCountry("US", "US"),
    SizeCountry("JP", "Japan", unit: "cm"), SizeCountry("AU", "Australia"), SizeCountry("KR", "Korea", unit: "mm")
]
let womensCountries: [SizeCountry] = [
    SizeCountry("US", "US"), SizeCountry("UK", "UK"), SizeCountry("EU", "EU"),
    SizeCountry("FR", "France"), SizeCountry("IT", "Italy"), SizeCountry("AU", "Australia"), SizeCountry("JP", "Japan")
]
let mensCountries: [SizeCountry] = [
    SizeCountry("US/UK", "US / UK"), SizeCountry("EU", "EU"),
    SizeCountry("JP", "Japan"), SizeCountry("AU", "Australia")
]

let menShoeTable: [[String: String]] = [
    ["EU": "38",   "UK": "5",    "US": "6",    "JP": "24.0", "AU": "5",    "KR": "240"],
    ["EU": "39",   "UK": "6",    "US": "7",    "JP": "24.5", "AU": "6",    "KR": "245"],
    ["EU": "40",   "UK": "6.5",  "US": "7.5",  "JP": "25.0", "AU": "6.5",  "KR": "250"],
    ["EU": "41",   "UK": "7",    "US": "8",    "JP": "25.5", "AU": "7",    "KR": "255"],
    ["EU": "42",   "UK": "8",    "US": "9",    "JP": "26.0", "AU": "8",    "KR": "260"],
    ["EU": "43",   "UK": "9",    "US": "10",   "JP": "27.0", "AU": "9",    "KR": "270"],
    ["EU": "44",   "UK": "9.5",  "US": "10.5", "JP": "27.5", "AU": "9.5",  "KR": "275"],
    ["EU": "45",   "UK": "10.5", "US": "11.5", "JP": "28.0", "AU": "10.5", "KR": "280"],
    ["EU": "46",   "UK": "11",   "US": "12",   "JP": "29.0", "AU": "11",   "KR": "290"],
    ["EU": "47",   "UK": "12",   "US": "13",   "JP": "30.0", "AU": "12",   "KR": "300"],
    ["EU": "48",   "UK": "13",   "US": "14",   "JP": "31.0", "AU": "13",   "KR": "310"],
]

let womenShoeTable: [[String: String]] = [
    ["EU": "35",   "UK": "2",   "US": "4",   "JP": "21.5", "AU": "2",   "KR": "215"],
    ["EU": "35.5", "UK": "2.5", "US": "4.5", "JP": "22.0", "AU": "2.5", "KR": "220"],
    ["EU": "36",   "UK": "3",   "US": "5",   "JP": "22.5", "AU": "3",   "KR": "225"],
    ["EU": "36.5", "UK": "3.5", "US": "5.5", "JP": "23.0", "AU": "3.5", "KR": "230"],
    ["EU": "37",   "UK": "4",   "US": "6",   "JP": "23.0", "AU": "4",   "KR": "235"],
    ["EU": "37.5", "UK": "4.5", "US": "6.5", "JP": "23.5", "AU": "4.5", "KR": "235"],
    ["EU": "38",   "UK": "5",   "US": "7",   "JP": "24.0", "AU": "5",   "KR": "240"],
    ["EU": "38.5", "UK": "5.5", "US": "7.5", "JP": "24.0", "AU": "5.5", "KR": "245"],
    ["EU": "39",   "UK": "6",   "US": "8",   "JP": "24.5", "AU": "6",   "KR": "245"],
    ["EU": "40",   "UK": "6.5", "US": "8.5", "JP": "25.0", "AU": "6.5", "KR": "250"],
    ["EU": "40.5", "UK": "7",   "US": "9",   "JP": "25.5", "AU": "7",   "KR": "255"],
    ["EU": "41",   "UK": "7.5", "US": "9.5", "JP": "25.5", "AU": "7.5", "KR": "260"],
    ["EU": "42",   "UK": "8",   "US": "10",  "JP": "26.0", "AU": "8",   "KR": "265"],
]

let womensClothingTable: [[String: String]] = [
    ["US": "0",  "UK": "4",  "EU": "32", "FR": "34", "IT": "36", "AU": "4",  "JP": "5"],
    ["US": "2",  "UK": "6",  "EU": "34", "FR": "36", "IT": "38", "AU": "6",  "JP": "7"],
    ["US": "4",  "UK": "8",  "EU": "36", "FR": "38", "IT": "40", "AU": "8",  "JP": "9"],
    ["US": "6",  "UK": "10", "EU": "38", "FR": "40", "IT": "42", "AU": "10", "JP": "11"],
    ["US": "8",  "UK": "12", "EU": "40", "FR": "42", "IT": "44", "AU": "12", "JP": "13"],
    ["US": "10", "UK": "14", "EU": "42", "FR": "44", "IT": "46", "AU": "14", "JP": "15"],
    ["US": "12", "UK": "16", "EU": "44", "FR": "46", "IT": "48", "AU": "16", "JP": "17"],
    ["US": "14", "UK": "18", "EU": "46", "FR": "48", "IT": "50", "AU": "18", "JP": "19"],
    ["US": "16", "UK": "20", "EU": "48", "FR": "50", "IT": "52", "AU": "20", "JP": "21"],
    ["US": "18", "UK": "22", "EU": "50", "FR": "52", "IT": "54", "AU": "22", "JP": "23"],
    ["US": "20", "UK": "24", "EU": "52", "FR": "54", "IT": "56", "AU": "24", "JP": "25"],
]

let mensClothingTable: [[String: String]] = [
    ["US/UK": "XS",  "EU": "44", "JP": "S",   "AU": "XS"],
    ["US/UK": "S",   "EU": "46", "JP": "M",   "AU": "S"],
    ["US/UK": "M",   "EU": "48", "JP": "L",   "AU": "M"],
    ["US/UK": "L",   "EU": "50", "JP": "XL",  "AU": "L"],
    ["US/UK": "XL",  "EU": "52", "JP": "XXL", "AU": "XL"],
    ["US/UK": "XXL", "EU": "54", "JP": "3XL", "AU": "XXL"],
    ["US/UK": "3XL", "EU": "56", "JP": "4XL", "AU": "3XL"],
]

func lookupSize(table: [[String: String]], fromKey: String, toKey: String, fromValue: String) -> String? {
    table.first { $0[fromKey] == fromValue }?[toKey]
}

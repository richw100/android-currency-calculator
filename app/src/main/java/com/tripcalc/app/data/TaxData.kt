package com.tripcalc.app.data

/**
 * Tax rates for informational display on included-tax countries.
 * Triple: (countryName, standardRate, reducedRates)
 */
val EU_VAT_RATES: Map<String, Triple<String, Double, List<Double>>> = mapOf(
    "JP" to Triple("Japan", 10.0, listOf(8.0)),
)

/**
 * US state base sales tax rates (Tax Foundation 2026 data).
 * Pair: (stateName, rate%). AK/DE/MT/NH/OR have no state sales tax (0.0).
 */
val US_STATE_TAX_RATES: Map<String, Pair<String, Double>> = mapOf(
    "AL" to Pair("Alabama",              4.0),
    "AK" to Pair("Alaska",               0.0),
    "AZ" to Pair("Arizona",              5.6),
    "AR" to Pair("Arkansas",             6.5),
    "CA" to Pair("California",           7.25),
    "CO" to Pair("Colorado",             2.9),
    "CT" to Pair("Connecticut",          6.35),
    "DC" to Pair("Washington D.C.",      6.0),
    "DE" to Pair("Delaware",             0.0),
    "FL" to Pair("Florida",              6.0),
    "GA" to Pair("Georgia",              4.0),
    "HI" to Pair("Hawaii",               4.0),
    "ID" to Pair("Idaho",                6.0),
    "IL" to Pair("Illinois",             6.25),
    "IN" to Pair("Indiana",              7.0),
    "IA" to Pair("Iowa",                 6.0),
    "KS" to Pair("Kansas",               6.5),
    "KY" to Pair("Kentucky",             6.0),
    "LA" to Pair("Louisiana",            4.45),
    "ME" to Pair("Maine",                5.5),
    "MD" to Pair("Maryland",             6.0),
    "MA" to Pair("Massachusetts",        6.25),
    "MI" to Pair("Michigan",             6.0),
    "MN" to Pair("Minnesota",            6.875),
    "MS" to Pair("Mississippi",          7.0),
    "MO" to Pair("Missouri",             4.225),
    "MT" to Pair("Montana",              0.0),
    "NE" to Pair("Nebraska",             5.5),
    "NV" to Pair("Nevada",               6.85),
    "NH" to Pair("New Hampshire",        0.0),
    "NJ" to Pair("New Jersey",           6.625),
    "NM" to Pair("New Mexico",           5.0),
    "NY" to Pair("New York",             4.0),
    "NC" to Pair("North Carolina",       4.75),
    "ND" to Pair("North Dakota",         5.0),
    "OH" to Pair("Ohio",                 5.75),
    "OK" to Pair("Oklahoma",             4.5),
    "OR" to Pair("Oregon",               0.0),
    "PA" to Pair("Pennsylvania",         6.0),
    "RI" to Pair("Rhode Island",         7.0),
    "SC" to Pair("South Carolina",       6.0),
    "SD" to Pair("South Dakota",         4.2),
    "TN" to Pair("Tennessee",            7.0),
    "TX" to Pair("Texas",                6.25),
    "UT" to Pair("Utah",                 6.1),
    "VT" to Pair("Vermont",              6.0),
    "VA" to Pair("Virginia",             5.3),
    "WA" to Pair("Washington",           6.5),
    "WV" to Pair("West Virginia",        6.0),
    "WI" to Pair("Wisconsin",            5.0),
    "WY" to Pair("Wyoming",              4.0),
)

/**
 * Canadian combined sales tax rates (GST + PST/HST/QST where applicable).
 * Pair: (provinceName, combinedRate%).
 */
val CANADA_PROVINCE_TAX_RATES: Map<String, Pair<String, Double>> = mapOf(
    "AB" to Pair("Alberta",                      5.0),
    "BC" to Pair("British Columbia",            12.0),
    "MB" to Pair("Manitoba",                    12.0),
    "NB" to Pair("New Brunswick",               15.0),
    "NL" to Pair("Newfoundland and Labrador",   15.0),
    "NT" to Pair("Northwest Territories",        5.0),
    "NS" to Pair("Nova Scotia",                 15.0),
    "NU" to Pair("Nunavut",                      5.0),
    "ON" to Pair("Ontario",                     13.0),
    "PE" to Pair("Prince Edward Island",        15.0),
    "QC" to Pair("Quebec",                      14.975),
    "SK" to Pair("Saskatchewan",                11.0),
    "YT" to Pair("Yukon",                        5.0),
)

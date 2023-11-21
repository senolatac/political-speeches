package com.fashiondigital.politicalspeeches.service

import com.fashiondigital.politicalspeeches.exception.EvaluationServiceException
import com.fashiondigital.politicalspeeches.model.ErrorCode
import com.fashiondigital.politicalspeeches.model.SpeakerStats
import com.fashiondigital.politicalspeeches.model.Speech
import com.fashiondigital.politicalspeeches.model.SpeechHeader
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVRecord
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpMethod
import org.springframework.http.client.ClientHttpResponse
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.function.Consumer
import kotlin.collections.HashMap


@Service
class CsvParserService(@Autowired val restTemplate: RestTemplate) : ICsvParserService {

    @Value("\${speech.target-year}")
    private val targetYear = 0

    @Value("\${speech.security-topic}")
    private val securityTopic: String? = null

    companion object {
        private val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd").withLocale(Locale.ENGLISH)
        private const val DELIMITER = ";"
    }

    //return <Speaker, Stats>
    override fun parseCSVsByUrls(urls: Set<String>): Map<String, SpeakerStats> {
        val speakerMap: MutableMap<String, SpeakerStats> = HashMap<String, SpeakerStats>()
        urls.forEach(Consumer { url: String -> parseCSV(url, speakerMap) })
        return speakerMap
    }

    //TODO: assumed there isn't any duplication on csv files.
    private fun parseCSV(url: String, speakerMap: MutableMap<String, SpeakerStats>) {
        val csvFormat = CSVFormat.DEFAULT.builder().setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreHeaderCase(true)
                .setIgnoreSurroundingSpaces(true)
                .setIgnoreEmptyLines(true)
                .setDelimiter(DELIMITER)
                .build()
        try {
            restTemplate.execute<Map<String, SpeakerStats>>(url, HttpMethod.GET, null, { clientHttpResponse: ClientHttpResponse ->
                try {
                    CSVParser.parse(clientHttpResponse.body, StandardCharsets.UTF_8, csvFormat).use { csvParser ->
                        //iterable is memory-friend. Don't call csvParser.getRecords() otherwise you can get OutOfMemoryError for large files.
                        for (csvRecord in csvParser) {
                            //no need to store all records, summarize and escape from extra memory
                            extractAndAddToSpeakerMap(csvRecord, speakerMap)
                        }
                        if (csvParser.headerNames.isEmpty() || csvParser.recordNumber == 0L) {
                            throw EvaluationServiceException(ErrorCode.CSV_PARSER_ERROR)
                        }
                    }
                } catch (ex: Exception) {
                    throw EvaluationServiceException(ErrorCode.CSV_PARSER_ERROR, ex)
                }
                speakerMap
            })
        } catch (ex: RestClientException) {
            throw EvaluationServiceException(ErrorCode.URL_READER_ERROR, ex)
        }
    }

    private fun extractAndAddToSpeakerMap(csvRecord: CSVRecord, speakerMap: MutableMap<String, SpeakerStats>) {
        val speech: Speech = Speech(
                speaker = csvRecord.get(SpeechHeader.SPEAKER.value),
                topic = csvRecord.get(SpeechHeader.TOPIC.value),
                date = LocalDate.parse(csvRecord.get(SpeechHeader.DATE.value), DATE_TIME_FORMATTER),
                words = csvRecord.get(SpeechHeader.WORDS.value).toInt())

        //Assumed, speech.date might be future so considered as valid case
        if (!StringUtils.hasText(speech.speaker) || !StringUtils.hasText(speech.topic)) {
            throw EvaluationServiceException(ErrorCode.CSV_PARSER_ERROR)
        }
        val speakerStats: SpeakerStats = SpeakerStats(
                targetYearSpeeches = if (speech.isTargetYear(targetYear)) 1 else 0, //increment 1
                securitySpeeches = if (speech.isSecurityTopic(securityTopic!!)) 1 else 0, //increment 1
                overallWords = speech.words
        )
        speakerMap[speech.speaker] = speakerStats.merge(speakerMap[speech.speaker])
    }
}


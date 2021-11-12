package com.conversant.app.wordish.data.xml

import com.conversant.app.wordish.model.Definition
import org.xml.sax.Attributes
import org.xml.sax.SAXException
import org.xml.sax.helpers.DefaultHandler

class SaxWordDefinitionsHandler : DefaultHandler() {
    private var stringBuilder: StringBuilder = StringBuilder()

    var wordsList: MutableList<Definition> = mutableListOf()

    private lateinit var definition: Definition

    private var wordTag: Boolean = false

    private var h1Tag: Boolean = false

    private var h2Tag: Boolean = false

    private var liTag: Boolean = false

    private var emTag: Boolean = false


    @Throws(SAXException::class)
    override fun startElement(
        uri: String,
        localName: String,
        qName: String,
        attributes: Attributes
    ) {
        when {
            XML_WORD_TAG.equals(qName, ignoreCase = true) -> {
                wordTag = true
                definition = Definition()
                stringBuilder.clear()
            }

            XML_H1_TAG.equals(qName, ignoreCase = true) -> {
                stringBuilder.append("<h1>")
                h1Tag = true
            }
            XML_H2_TAG.equals(qName, ignoreCase = true) -> {
                stringBuilder.append("<h2>")
                h2Tag = true
            }
            XML_OL_TAG.equals(qName, ignoreCase = true) -> {
                stringBuilder.append("<ol>")
            }
            XML_LI_TAG.equals(qName, ignoreCase = true) -> {
                liTag = true
                stringBuilder.append("<li>")
            }
            XML_EM_TAG.equals(qName, ignoreCase = true) -> {
                emTag = true
                stringBuilder.append("<em>")
            }
        }
    }

    override fun endElement(uri: String?, localName: String?, qName: String?) {
        if (XML_WORD_TAG.equals(qName, ignoreCase = true)) {
            wordTag = false
        } else if (XML_H1_TAG.equals(qName, ignoreCase = true)) {
            stringBuilder.append("</h1>")
            h1Tag = false
        } else if (XML_H2_TAG.equals(qName, ignoreCase = true)) {
            stringBuilder.append("</h2>")
            h2Tag = false
        } else if (XML_OL_TAG.equals(qName, ignoreCase = true)) {
            stringBuilder.append("</ol>")
        } else if (XML_EM_TAG.equals(qName, ignoreCase = true)) {
            stringBuilder.append("</em>")
            emTag = false

        } else if (XML_LI_TAG.equals(qName, ignoreCase = true)) {
            stringBuilder.append("</li>")
            liTag = false

        } else if (XML_DEFINITION_TAG.equals(qName, ignoreCase = true)) {
            if (stringBuilder.length > 1) {
                val define = stringBuilder.toString()
                definition.definitionXml = define
                wordsList.add(definition)
            }
        }
        super.endElement(uri, localName, qName)
    }

    override fun characters(ch: CharArray?, start: Int, length: Int) {
        if (wordTag && length != 1) {
            definition.word = String(ch!!, start, length)
        } else if (h1Tag && length != 1) {
            val h1Value = String(ch!!, start, length)
            stringBuilder.append(h1Value)

        } else if (h2Tag && length != 1) {
            val h2Value = String(ch!!, start, length)
            stringBuilder.append(h2Value)

        } else if (liTag) {
            if (emTag && length != 1) {
                val emValue = String(ch!!, start, length)
                stringBuilder.append(emValue)
            } else {
                val liValue = String(ch!!, start, length)
                stringBuilder.append(liValue)
            }

        } else if (length != 1) {
            val liValue = String(ch!!, start, length)
            stringBuilder.append(liValue)
        }

        super.characters(ch, start, length)

    }

    companion object {
        private const val XML_WORD_TAG = "word"
        private const val XML_DEFINITION_TAG = "definitions"
        private const val XML_H1_TAG = "h1"
        private const val XML_H2_TAG = "h2"
        private const val XML_OL_TAG = "ol"
        private const val XML_LI_TAG = "li"
        private const val XML_EM_TAG = "em"

    }
}

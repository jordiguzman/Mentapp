package mentat.music.com.mentapp.data.remote.dto

import com.tickaroo.tikxml.annotation.Element
import com.tickaroo.tikxml.annotation.PropertyElement
import com.tickaroo.tikxml.annotation.Xml

// 1. La raíz del XML
@Xml(name = "rss")
data class RssFeedDto(
    @field:Element(name = "channel")
    var channel: RssChannelDto? = null // <--- Cambiado a VAR
)

// 2. El canal
@Xml(name = "channel")
data class RssChannelDto(
    @field:PropertyElement(name = "title")
    var title: String? = null, // <--- Cambiado a VAR

    @field:Element(name = "item")
    var items: List<RssItemDto>? = null // <--- Cambiado a VAR
)

// 3. Cada noticia individual
@Xml(name = "item")
data class RssItemDto(
    @field:PropertyElement(name = "title")
    var title: String? = null, // <--- VAR

    @field:PropertyElement(name = "link")
    var link: String? = null, // <--- VAR

    @field:PropertyElement(name = "content:encoded")
    var content: String? = null, // <--- VAR

    @field:PropertyElement(name = "description")
    var description: String? = null, // <--- VAR

    @field:PropertyElement(name = "pubDate")
    var pubDate: String? = null // <--- VAR
)
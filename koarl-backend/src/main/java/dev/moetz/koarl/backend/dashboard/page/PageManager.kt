package dev.moetz.koarl.backend.dashboard.page

import dev.moetz.koarl.backend.environment.EnvironmentVariable
import dev.moetz.koarl.backend.environment.dashboardUrl

class PageManager(
    @PublishedApi internal val environmentVariable: EnvironmentVariable
) {

    class PageBuilder
    @PublishedApi internal constructor(
        @PublishedApi internal val stringBuilder: StringBuilder,
        @PublishedApi internal val environmentVariable: EnvironmentVariable
    ) {

        data class NavigationLink(
            val name: String,
            val href: String,
            val isActive: Boolean
        )

        fun navigation(logoName: String, links: List<NavigationLink>) {
            stringBuilder.append("<nav class=\"light-blue lighten-1\" role=\"navigation\">")
            stringBuilder.append(
                "<div class=\"nav-wrapper container\"><a id=\"logo-container\" href=\"${EnvironmentVariable.Key.Application.appendToUrl(
                    environmentVariable,
                    "dashboard"
                )}\" class=\"brand-logo\">$logoName</a>"
            )
            stringBuilder.append("<ul class=\"right hide-on-med-and-down\">")
            links.forEach { navigationLink ->
                stringBuilder.append(
                    "<li${if (navigationLink.isActive) " class=\"active\"" else ""}><a href=\"${navigationLink.href}\">${navigationLink.name}</a></li>"
                )
            }

            stringBuilder.append(
                "</ul>" +
                        "<ul id=\"nav-mobile\" class=\"sidenav\">"
            )
            links.forEach { navigationLink ->
                stringBuilder.append(
                    "<li${if (navigationLink.isActive) " class=\"active\"" else ""}><a href=\"${navigationLink.href}\">${navigationLink.name}</a></li>"
                )
            }
            stringBuilder.append(
                "</ul>" +
                        "<a href=\"#\" data-target=\"nav-mobile\" class=\"sidenav-trigger\"><i class=\"material-icons\">menu</i></a>" +
                        "</div>" +
                        "</nav>"
            )
        }


        @PublishedApi
        internal val html = HtmlContent(stringBuilder)

        suspend inline fun html(block: HtmlContent.() -> Unit) {
            block.invoke(html)
        }

        fun footer(url: String?) {
            html.footer(listOf("page-footer", "orange")) {
                div(listOf("footer-copyright")) {
                    div(listOf("container")) {
                        div(listOf("row")) {
                            div(listOf("left")) {
                                a(
                                    classes = listOf("white-text"),
                                    href = "https://github.com/FlowMo7/Koarl",
                                    text = "Koarl is Open Source"
                                )
                            }
                            div(listOf("right")) {
                                text("Made with&nbsp;")
                                a(
                                    classes = listOf("orange-text", "text-lighten-3"),
                                    href = "http://materializecss.com",
                                    text = "Materialize"
                                )
                            }
                        }
                        div(listOf("row")) {
                            div(listOf("right")) {
                                if (url != null) {
                                    text("This page as&nbsp;")
                                    a(
                                        classes = listOf("orange-text", "text-lighten-3"),
                                        href = "/dashboard/api/$url",
                                        text = "API"
                                    )
                                    if (environmentVariable.getBoolean(EnvironmentVariable.Key.Swagger.Enable) == true) {
                                        text("&nbsp;(check out the&nbsp;")
                                        a(
                                            classes = listOf("orange-text", "text-lighten-3"),
                                            href = EnvironmentVariable.Key.Application.appendToUrl(
                                                environmentVariable,
                                                "swagger"
                                            ),
                                            text = "Swagger Documentation"
                                        )
                                        text(")")
                                    } else {
                                        text("&nbsp;(Swagger-Documentation has been disabled by the Server Admin)")
                                    }
                                } else {
                                    a(
                                        classes = listOf("orange-text", "text-lighten-3"),
                                        href = EnvironmentVariable.Key.Application.appendToUrl(
                                            environmentVariable,
                                            "swagger"
                                        ),
                                        text = "Swagger Documentation"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    class HtmlContent
    @PublishedApi internal constructor(@PublishedApi internal val stringBuilder: StringBuilder) {

        fun h1(text: String, classes: List<String> = emptyList()) {
            elementWithClasses("h1", text, classes)
        }

        fun h2(text: String, classes: List<String> = emptyList()) {
            elementWithClasses("h2", text, classes)
        }

        fun h3(text: String, classes: List<String> = emptyList()) {
            elementWithClasses("h3", text, classes)
        }

        fun h4(text: String, classes: List<String> = emptyList()) {
            elementWithClasses("h4", text, classes)
        }

        fun h5(text: String, classes: List<String> = emptyList()) {
            elementWithClasses("h5", text, classes)
        }

        fun icon(iconIdentifier: String) {
            elementWithClasses("i", content = iconIdentifier, classes = listOf("material-icons"))
        }

        fun p(text: String, classes: List<String> = emptyList()) {
            elementWithClasses("p", content = text, classes = classes)
        }


        data class BreadcrumbItem(
            val href: String,
            val title: String
        )

        suspend fun breadcrumbs(breadcrumbs: List<BreadcrumbItem>) {
            div(classes = listOf("row")) {
                stringBuilder.append("<nav>")
                stringBuilder.append("<div class=\"nav-wrapper\">")
                stringBuilder.append("<div class=\"col s8 center\">")
                breadcrumbs.forEach { crumb ->
                    stringBuilder.append("<a href=\"${crumb.href}\" class=\"breadcrumb\">${crumb.title}</a>")
                }
                stringBuilder.append("</div>")
                stringBuilder.append("</div>")
                stringBuilder.append("</nav>")
            }
        }


        inline fun footer(classes: List<String>, content: HtmlContent.() -> Unit) {
            stringBuilder.append("<footer ${classes.asHtmlClassDefinition}>")
            content.invoke(HtmlContent(stringBuilder))
            stringBuilder.append("</footer>")
        }

        fun a(classes: List<String>, href: String, text: String) {
            stringBuilder.append("<a ${classes.asHtmlClassDefinition} href=\"$href\">$text</a>")
        }

        fun breakLine() {
            stringBuilder.append("<br />")
        }

        fun text(text: String) {
            stringBuilder.append(text)
        }

        fun span(classes: List<String>, text: String) {
            stringBuilder.append("<span ${classes.asHtmlClassDefinition}>$text</span>")
        }

        fun pre(classes: List<String>, text: String) {
            stringBuilder.append("<pre ${classes.asHtmlClassDefinition} style=\"overflow-x:scroll;\">$text</pre>")
        }

        fun newBadge(caption: String, text: String, color: String? = null) {
            stringBuilder.append("<span class=\"new badge")
            if (color != null) {
                stringBuilder.append(" $color")
            }
            stringBuilder.append("\" data-badge-caption=\"$caption\">$text</span>")
        }

        fun flatButton(href: String, text: String, enabled: Boolean = true) {
            stringBuilder.append("<a class=\"waves-effect waves-light btn-flat")
            if (enabled.not()) {
                stringBuilder.append(" disabled")
            }
            stringBuilder.append("\" href=\"$href\">$text</a>")
        }

        fun button(href: String, text: String, enabled: Boolean = true) {
            stringBuilder.append("<a class=\"waves-effect waves-light btn")
            if (enabled.not()) {
                stringBuilder.append(" disabled")
            }
            stringBuilder.append("\" href=\"$href\">$text</a>")
        }

        fun submitButton(name: String, label: String) {
            stringBuilder.append("<button class=\"btn waves-effect waves-light\" type=\"submit\" name=\"$name\">")
            stringBuilder.append(label)
            stringBuilder.append("<i class=\"material-icons right\">send</i>")
            stringBuilder.append("</button>")
        }


        @PublishedApi
        internal val List<String>.asHtmlClassDefinition: String
            get() = if (this.isNotEmpty()) "class=\"${this.joinToString(separator = " ")}\"" else ""


        inline fun div(classes: List<String>, content: HtmlContent.() -> Unit) {
            stringBuilder.append("<div ${classes.asHtmlClassDefinition}>")
            content.invoke(HtmlContent(stringBuilder))
            stringBuilder.append("</div>")
        }


        inline fun <T> table(
            classes: List<String>,
            headers: List<String>,
            items: List<T>,
            itemToHtml: HtmlContent.(Int, T) -> Unit
        ) {
            val numberOfColumns = headers.size

            stringBuilder.append("<table ${classes.asHtmlClassDefinition}\">")

            stringBuilder.append("<thead>")
            stringBuilder.append("<tr>")
            headers.forEach { headerItem ->
                stringBuilder.append("<th>$headerItem</th>")
            }
            stringBuilder.append("</tr>")
            stringBuilder.append("</thead>")


            stringBuilder.append("<tbody>")
            items.forEach { row ->
                stringBuilder.append("<tr>")

                (0..numberOfColumns).forEach { index ->
                    stringBuilder.append("<td>")
                    itemToHtml.invoke(HtmlContent(stringBuilder), index, row)
                    stringBuilder.append("</td>")
                }
                stringBuilder.append("</tr>")

            }
            stringBuilder.append("</tbody>")
            stringBuilder.append("</table>")
        }


        suspend inline fun cardPanel(color: String, content: HtmlContent.() -> Unit) {
            stringBuilder.append("<div class=\"card-panel $color\">")
            content.invoke(HtmlContent(stringBuilder))
            stringBuilder.append("</div>")
        }

        suspend inline fun form(action: String, method: String, block: HtmlContent.() -> Unit) {
            stringBuilder.append("<form action=\"$action\" method=\"$method\" enctype=\"multipart/form-data\">")
            block.invoke(HtmlContent(stringBuilder))
            stringBuilder.append("</form>")
        }


        fun fileUpload(label: String, name: String) {
            stringBuilder.append("<div class=\"file-field input-field\">")
            stringBuilder.append("<div class=\"btn\"><span>$label</span><input type=\"file\" name=\"$name\" id=\"$name\"></div>")
            stringBuilder.append("<div class=\"file-path-wrapper\">")
            stringBuilder.append("<input class=\"file-path validate\" type=\"text\">")
            stringBuilder.append("</div>")
            stringBuilder.append("</div>")
        }

        fun textInput(
            classes: List<String>,
            placeholder: String?,
            id: String,
            type: String,
            hint: String
        ) {
            stringBuilder.append("<div ${(listOf("input-field") + classes).asHtmlClassDefinition}>")
            stringBuilder.append("<input placeholder=\"$placeholder\" id=\"$id\" name=\"$id\" type=\"$type\" class=\"validate\">")
            stringBuilder.append("<label for=\"$id\">$hint</label>")
            stringBuilder.append("</div>")
        }

        data class MultiSelectItem(
            val value: String,
            val name: String
        )

        fun multiSelect(id: String, label: String, options: List<MultiSelectItem>) {
            stringBuilder.append("<div class=\"input-field\">")
            stringBuilder.append("<select id=\"$id\" name=\"$id\" multiple>")
            options.forEach { item ->
                stringBuilder.append("<option value=\"${item.value}\">${item.name}</option>")
            }
            stringBuilder.append("</select>")
            stringBuilder.append("<label for=\"$id\">$label</label>")
            stringBuilder.append("</div>")
        }


        data class LinkedCollectionItem(
            val href: String,
            val text: String
        )

        fun linkedCollection(
            items: List<LinkedCollectionItem>
        ) {
            stringBuilder.append("<div class=\"collection\">")
            items.forEach { item ->
                stringBuilder.append("<a href=\"${item.href}\" class=\"collection-item\">${item.text}</a>")
            }
            stringBuilder.append("</div>")
        }


        inline fun pagination(
            numberOfPages: Int,
            activePage: Int,
            maxDisplayedPages: Int,
            linkBuilder: (page: Int) -> String
        ) {
            val paginationResult = PaginationManager().getPaginatedPagesToDisplay(
                numberOfPages = numberOfPages,
                activePage = activePage,
                maxDisplayedPages = maxDisplayedPages
            )

            stringBuilder.append("<ul class=\"pagination\">")

            if (paginationResult.rangeStart == activePage) {
                stringBuilder.append("<li class=\"disabled\"><a disabled=\"disabled\"><i class=\"material-icons\">chevron_left</i></a></li>")
            } else {
                stringBuilder.append(
                    "<li class=\"waves-effect\"><a href=\"${linkBuilder.invoke(activePage - 1)}\"><i class=\"material-icons\">chevron_left</i></a></li>"
                )
            }

            val list = mutableListOf<Int>()
            if (paginationResult.rangeStart != paginationResult.allPagesStart) {
                list.add(paginationResult.allPagesStart)
            }
            (paginationResult.rangeStart..paginationResult.rangeEnd).forEach { list.add(it) }
            if (paginationResult.rangeEnd != paginationResult.allPagesEnd) {
                list.add(paginationResult.allPagesEnd)
            }

            if (paginationResult.rangeStart != paginationResult.allPagesStart && paginationResult.rangeStart - 1 != paginationResult.allPagesStart) {
                list[1] = -1
            }
            if (paginationResult.rangeEnd != paginationResult.allPagesEnd && paginationResult.rangeEnd + 1 != paginationResult.allPagesEnd) {
                list[list.lastIndex - 1] = -1
            }

            list.forEach { page ->
                when (page) {
                    -1 -> stringBuilder.append("<li class=\"waves-effect\"><a disabled=\"disabled\">...</a></li>")
                    activePage -> stringBuilder.append("<li class=\"active\"><a disabled=\"disabled\">$page</a></li>")
                    else -> stringBuilder.append(
                        "<li class=\"waves-effect\">" +
                                "<a href=\"${linkBuilder.invoke(page)}\">$page</a>" +
                                "</li>"
                    )
                }
            }


            if (activePage == paginationResult.rangeEnd) {
                stringBuilder.append("<li class=\"disabled\"><a disabled=\"disabled\"><i class=\"material-icons\">chevron_right</i></a></li>")
            } else {
                stringBuilder.append(
                    "<li class=\"waves-effect\">" +
                            "<a href=\"${linkBuilder.invoke(paginationResult.rangeEnd)}\">" +
                            "<i class=\"material-icons\">chevron_right</i>" +
                            "</a>" +
                            "</li>"
                )
            }
        }


        private fun elementWithClasses(
            element: String,
            content: String,
            classes: List<String> = emptyList()
        ) {
            stringBuilder.append("<$element ${classes.asHtmlClassDefinition}\">$content</$element>")
        }


        suspend inline fun collapsible(
            numberOfItems: Int,
            block: HtmlContent.(itemIndex: Int, isHeader: Boolean) -> Unit
        ) {
            stringBuilder.append("<ul class=\"collapsible\">")

            (0 until numberOfItems).forEach { itemIndex ->
                stringBuilder.append("<li>")

                stringBuilder.append("<div class=\"collapsible-header\">")
                block.invoke(HtmlContent(stringBuilder = stringBuilder), itemIndex, true)
                stringBuilder.append("</div>")

                stringBuilder.append("<div class=\"collapsible-body\">")
                block.invoke(HtmlContent(stringBuilder = stringBuilder), itemIndex, false)
                stringBuilder.append("</div>")

                stringBuilder.append("</li>")
            }

            stringBuilder.append("</ul>")
        }
    }


    suspend inline fun build(title: String, block: PageBuilder.() -> Unit): String {
        val stringBuilder = StringBuilder("<!DOCTYPE html>\n")
        stringBuilder.append(
            "<html lang=\"en\">" +
                    "<head>" +
                    "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/>" +
                    "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1, maximum-scale=1.0\"/>" +
                    "<title>$title</title>" +
                    "<link href=\"${environmentVariable.dashboardUrl("static/MaterialIcons.css")}\" rel=\"stylesheet\">" +
                    "<link href=\"${environmentVariable.dashboardUrl("static/materialize.min.css")}\" type=\"text/css\" rel=\"stylesheet\" media=\"screen,projection\"/>" +
                    "</head>"
        )
        stringBuilder.append("<body>")


        block.invoke(PageBuilder(stringBuilder, environmentVariable))



        stringBuilder.append("<script src=\"")
            .append(environmentVariable.dashboardUrl("static/jquery-2.1.1.min.js"))
            .append("\"></script>")
            .append("<script src=\"")
            .append(environmentVariable.dashboardUrl("static/materialize.min.js"))
            .append("\"></script>")
            .append("<script src=\"")
            .append(environmentVariable.dashboardUrl("static/init.js"))
            .append("\"></script>")
            .append("</body>")
            .append("</html>")

        return stringBuilder.toString()
    }


}
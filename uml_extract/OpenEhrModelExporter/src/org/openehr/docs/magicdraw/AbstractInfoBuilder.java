package org.openehr.docs.magicdraw;

import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Comment;
import com.nomagic.uml2.ext.magicdraw.classes.mdkernel.Element;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Bostjan Lah
 */
public abstract class AbstractInfoBuilder<T> {
    @SuppressWarnings("HardcodedLineSeparator")
    protected String getDocumentation(Element element, Formatter formatter) {
        return String.join(formatter.newParagraph(), element.getOwnedComment().stream()
                .map(Comment::getBody)
                .flatMap(body -> Stream.of(body.split("\n")))
                .filter(line -> !line.trim().isEmpty())
                .collect(Collectors.toList()));
    }

    public abstract ClassInfo build(T element);
}

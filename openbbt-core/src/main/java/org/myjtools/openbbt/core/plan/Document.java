package org.myjtools.openbbt.core.plan;

import java.util.function.UnaryOperator;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
public final class Document implements NodeArgument {


    public static Document of(String contentType, String content) {
        if (content == null || content.isBlank()) {
            return null;
        }
        return new Document(contentType,content);
    }


    private String contentType;
    private String content;

    @Override
    public NodeArgument copy(UnaryOperator<String> replacingVariablesMethod) {
        return new Document(contentType, replacingVariablesMethod.apply(content));
    }


    public String contentType() {
        return contentType;
    }

}

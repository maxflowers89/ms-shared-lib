package com.illimity.rts.commonconfiglib.types.logging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Parameter {

  private String type;
  private String name;
  private Object value;
}

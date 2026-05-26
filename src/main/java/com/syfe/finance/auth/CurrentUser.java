package com.syfe.finance.auth;

import java.io.Serializable;

public record CurrentUser(Long id, String username) implements Serializable {
}

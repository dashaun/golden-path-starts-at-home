#!/usr/bin/env bash
# Use the JDK this project pins in .sdkmanrc, so a shell without sdkman's
# auto-env still builds and runs the same way everyone else's does.

golden::use_pinned_java() {
  local root="$1"
  local rc="${root}/.sdkmanrc"
  [[ -f "${rc}" ]] || return 0

  local version
  version="$(grep -E '^java=' "${rc}" | head -1 | cut -d= -f2 | tr -d '[:space:]')"
  [[ -n "${version}" ]] || return 0

  local candidate="${SDKMAN_DIR:-${HOME}/.sdkman}/candidates/java/${version}"
  if [[ -d "${candidate}" ]]; then
    export JAVA_HOME="${candidate}"
    export PATH="${JAVA_HOME}/bin:${PATH}"
  else
    echo "Note: .sdkmanrc asks for java ${version}, which is not installed." >&2
    echo "      Run 'sdk env install' for a build that matches everyone else's." >&2
  fi
}

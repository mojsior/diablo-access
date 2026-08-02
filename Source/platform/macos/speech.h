#pragma once

#include <string_view>

namespace devilution {

void osx_speak_text(std::string_view text, bool force);
void osx_init_speech();
void osx_shutdown_speech();

} // namespace devilution

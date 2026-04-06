#include "utils/screen_reader.hpp"

#include <string>
#include <string_view>
#include "options.h" // Required to access GetOptions() and SaveOptions()

#ifdef _WIN32
#include "utils/file_util.h"
#include <Tolk.h>
#elif defined(__APPLE__)
#include "platform/macos/speech.h"
#else
#include <speech-dispatcher/libspeechd.h>
#endif

namespace devilution {

#if !defined(_WIN32) && !defined(__APPLE__)
SPDConnection *Speechd;
#endif

void InitializeScreenReader()
{
#ifdef _WIN32
	Tolk_Load();
#elif defined(__APPLE__)
	osx_init_speech();
#else
	Speechd = spd_open("DevilutionX", "DevilutionX", NULL, SPD_MODE_SINGLE);
#endif
}

void ShutDownScreenReader()
{
#ifdef _WIN32
	Tolk_Unload();
#elif defined(__APPLE__)
	osx_shutdown_speech();
#else
	spd_close(Speechd);
#endif
}

void SpeakText(std::string_view text, bool force)
{
	static std::string SpokenText;

	if (!force && SpokenText == text)
		return;

	SpokenText = text;

#ifdef _WIN32
	const auto textUtf16 = ToWideChar(SpokenText);
	if (textUtf16 != nullptr)
		Tolk_Output(textUtf16.get(), true);
#elif defined(__APPLE__)
	osx_speak_text(text, force);
#else
	spd_say(Speechd, SPD_TEXT, SpokenText.c_str());
#endif
}

namespace {
void SaySpeechRate()
{
	float currentRate = GetOptions().Audio.speechRate.GetValue();
	std::string rateText = fmt::format(N_("Speech rate {:.2f}"), currentRate);
	SpeakText(rateText, true);
}
} // namespace

void IncreaseSpeechRate()
{
	auto &speechRateOption = GetOptions().Audio.speechRate;
	// Find current index in entryValues
	size_t currentIndex = speechRateOption.GetActiveListIndex();
	if (currentIndex < speechRateOption.GetListSize() - 1) {
		speechRateOption.SetActiveListIndex(currentIndex + 1);
		SaveOptions();
		SaySpeechRate();
	} else {
		SpeakText(N_("Maximum speech rate"), true);
	}
}

void DecreaseSpeechRate()
{
	auto &speechRateOption = GetOptions().Audio.speechRate;
	// Find current index in entryValues
	size_t currentIndex = speechRateOption.GetActiveListIndex();
	if (currentIndex > 0) {
		speechRateOption.SetActiveListIndex(currentIndex - 1);
		SaveOptions();
		SaySpeechRate();
	} else {
		SpeakText(N_("Minimum speech rate"), true);
	}
}

} // namespace devilution

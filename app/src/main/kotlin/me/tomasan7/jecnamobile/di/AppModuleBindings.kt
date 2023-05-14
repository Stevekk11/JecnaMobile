package me.tomasan7.jecnamobile.di

import dagger.Binds
import dagger.Module
import dagger.hilt.migration.DisableInstallInCheck
import me.tomasan7.jecnamobile.attendances.AttendancesRepository
import me.tomasan7.jecnamobile.attendances.AttendancesRepositoryImpl
import me.tomasan7.jecnamobile.grades.GradesRepository
import me.tomasan7.jecnamobile.grades.GradesRepositoryImpl
import me.tomasan7.jecnamobile.login.AuthRepository
import me.tomasan7.jecnamobile.login.CanteenServerPasswordRepository
import me.tomasan7.jecnamobile.login.FileCanteenServerPasswordRepository
import me.tomasan7.jecnamobile.login.SharedPreferencesAuthRepository
import me.tomasan7.jecnamobile.news.NewsRepository
import me.tomasan7.jecnamobile.news.NewsRepositoryImpl
import me.tomasan7.jecnamobile.teachers.TeachersRepository
import me.tomasan7.jecnamobile.teachers.TeachersRepositoryImpl
import me.tomasan7.jecnamobile.timetable.TimetableRepository
import me.tomasan7.jecnamobile.timetable.TimetableRepositoryImpl
import javax.inject.Singleton

@DisableInstallInCheck
@Module
interface AppModuleBindings
{
    @Binds
    @Singleton
    fun bindAuthRepository(repository: SharedPreferencesAuthRepository): AuthRepository

    @Binds
    @Singleton
    fun bindCanteenServerPasswordRepository(repository: FileCanteenServerPasswordRepository): CanteenServerPasswordRepository

    @Binds
    @Singleton
    fun bindGradesRepository(repository: GradesRepositoryImpl): GradesRepository

    @Binds
    @Singleton
    fun bindTimetableRepository(repository: TimetableRepositoryImpl): TimetableRepository

    @Binds
    @Singleton
    fun bindNewsRepository(repository: NewsRepositoryImpl): NewsRepository

    @Binds
    @Singleton
    fun bindAttendancesRepository(repository: AttendancesRepositoryImpl): AttendancesRepository

    @Binds
    @Singleton
    fun bindTeachersRepository(repository: TeachersRepositoryImpl): TeachersRepository
}
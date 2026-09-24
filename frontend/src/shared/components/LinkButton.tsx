import type { ComponentProps } from 'react'
import { Link } from 'react-router-dom'
import { buttonClassName, type ButtonStyleProps } from './buttonStyles'

interface LinkButtonProps
  extends ComponentProps<typeof Link>, ButtonStyleProps {}

/** Mismo look que {@link Button}, para navegación en vez de una acción (usa react-router). */
export function LinkButton({
  variant,
  small,
  className,
  ...props
}: LinkButtonProps) {
  return (
    <Link
      className={buttonClassName({ variant, small }, className)}
      {...props}
    />
  )
}
